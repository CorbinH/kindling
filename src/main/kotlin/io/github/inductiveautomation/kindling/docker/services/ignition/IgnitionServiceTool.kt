package io.github.inductiveautomation.kindling.docker.services.ignition

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.docker.DockerDraftPanel
import io.github.inductiveautomation.kindling.docker.services.DockerServiceTool
import io.github.inductiveautomation.kindling.docker.services.ignition.model.IgnitionCommandLineArgument
import io.github.inductiveautomation.kindling.docker.services.ignition.model.IgnitionServiceModel
import io.github.inductiveautomation.kindling.docker.services.ignition.model.IgnitionStaticDefinition
import io.github.inductiveautomation.kindling.docker.services.ignition.model.IgnitionVersionComparator
import io.github.inductiveautomation.kindling.docker.services.model.DefaultDockerServiceModel
import io.github.inductiveautomation.kindling.docker.services.model.DockerEnvironmentVariableDefinition.Companion.getConnectionVariableIndex
import io.github.inductiveautomation.kindling.docker.services.model.DockerServiceModel
import io.github.inductiveautomation.kindling.docker.services.model.PortMapping
import io.github.inductiveautomation.kindling.docker.volumes.model.BindMount
import io.github.inductiveautomation.kindling.statistics.GatewayBackup
import io.github.inductiveautomation.kindling.statistics.categories.MetaStatistics
import io.github.inductiveautomation.kindling.utils.MajorVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object IgnitionServiceTool : DockerServiceTool {
    override val icon = FlatSVGIcon("icons/Logo-Ignition-Check.svg")
    override val name = "Ignition Gateway Node"
    override val defaultImage = "inductiveautomation/ignition:latest"

    private const val DOCKER_BASE =
        "https://hub.docker.com/v2/repositories/inductiveautomation/ignition/tags?ordering=last_updated&page=1"
    private const val PEEK_URL = "$DOCKER_BASE&page_size=1"
    private const val FULL_URL = "$DOCKER_BASE&page_size=1000"

    context(panel: DockerDraftPanel)
    override fun createModel(): IgnitionServiceModel = modelFromDefault(
        DefaultDockerServiceModel(
            image = defaultImage,
            containerName = "Ignition-${panel.nodeIdManager.generateID()}",
            ports = mutableListOf(PortMapping(panel.defaultPortManager.requestPorts(1).single().toString(), "8088")),
        ),
    )

    context(panel: DockerDraftPanel)
    override fun createNode(model: DockerServiceModel): IgnitionServiceNode {
        require(model is IgnitionServiceModel) {
            "Model ${model::class.java.name} is not an Ignition Service Model"
        }

        val observer = panel.serviceData["connectionObserver"] as ConnectionObserver?

        return IgnitionServiceNode(model, panel.volumes, panel.networks.keys.toList()).apply {
            observer?.observeConnection(this)
        }
    }

    override fun DockerDraftPanel.init() {
        val observer = ConnectionObserver(canvas)
        serviceData["connectionObserver"] = observer

        val ignitionServices = services.filterIsInstance<IgnitionServiceNode>()
        val connectors = resolveConnections(ignitionServices)

        for (c in connectors) {
            canvas.add(c)
        }

        for (service in ignitionServices) {
            observer.observeConnection(service)
        }
    }

    override fun isValidCandidate(model: DefaultDockerServiceModel): Boolean = model.image.startsWith("inductiveautomation/ignition")

    override fun modelFromDefault(model: DefaultDockerServiceModel) = IgnitionServiceModel(model)

    context(panel: DockerDraftPanel)
    fun createModelFromGwbk(path: Path): IgnitionServiceModel {
        val backup = GatewayBackup(path)
        val gwbkHttpPort = backup.gatewaySettings.getProperty("gateway.port", "8088")
        val gwbkHttpsPort = backup.gatewaySettings.getProperty("gateway.sslport", "8043")
        val forceSecureRedirect = backup.gatewaySettings.getProperty("gateway.forceSecureRedirect", "false").toBoolean()

        val version = backup.info.getElementsByTagName("version").item(0)?.textContent
            ?.split(".")?.take(3)?.joinToString(".")
            ?.takeIf { it.isNotBlank() } ?: "latest"

        val hasConflict = gwbkHttpPort != "8088" || gwbkHttpsPort != "8043" || forceSecureRedirect
        val strategy = if (hasConflict) {
            showPortConflictDialog(panel, gwbkHttpPort, gwbkHttpsPort, forceSecureRedirect) ?: PortStrategy.KEEP
        } else {
            PortStrategy.KEEP
        }

        val httpTarget = if (strategy == PortStrategy.RESET) "8088" else gwbkHttpPort
        val httpsTarget = if (strategy == PortStrategy.RESET) "8043" else gwbkHttpsPort

        val requestedPorts = panel.defaultPortManager.requestPorts(if (forceSecureRedirect) 2 else 1)
        val portMappings = mutableListOf(
            PortMapping(requestedPorts[0].toString(), httpTarget),
        )
        if (forceSecureRedirect) {
            portMappings.add(PortMapping(requestedPorts[1].toString(), httpsTarget))
        }

        val gatewayName = runBlocking {
            val stats = MetaStatistics[MajorVersion.lookup(version)]?.calculate(backup)
            stats?.gatewayName
        }

        val containerName = gatewayName ?: "Ignition-${panel.nodeIdManager.generateID()}"
        val restorePath = "/restore-$containerName.gwbk"

        val model = modelFromDefault(
            DefaultDockerServiceModel(
                image = "inductiveautomation/ignition:$version",
                containerName = containerName,
                hostName = containerName,
                ports = portMappings,
                volumes = mutableListOf(
                    BindMount(
                        bindPath = path.absolutePathString(),
                        containerPath = restorePath,
                    ),
                ),
                commands = mutableListOf("${IgnitionCommandLineArgument.GWBK_RESTORE_PATH.flag} $restorePath"),
            ),
        )

        if (strategy == PortStrategy.KEEP) {
            // Without these env vars, the Ignition image forces the gateway to default ports (8088/8043)
            // on startup, overriding whatever was in the restored GWBK. Setting them explicitly preserves
            // the GWBK's port configuration.
            model.environment[IgnitionStaticDefinition.GATEWAY_HTTP_PORT.name] = gwbkHttpPort
            model.environment[IgnitionStaticDefinition.GATEWAY_HTTPS_PORT.name] = gwbkHttpsPort
        }

        return model
    }

    fun DockerDraftPanel.resolveConnections(nodes: List<IgnitionServiceNode>): List<IgnitionNodeConnector> {
        val connections = mutableListOf<IgnitionNodeConnector>()

        for (node in nodes) {
            val outboundHosts = node.model.environment.filter { (k, _) ->
                k.startsWith("GATEWAY_NETWORK_") && k.endsWith("_HOST")
            }.map {
                it.key.getConnectionVariableIndex()!! to it.value
            }

            for (host in outboundHosts) {
                val outboundNode = nodes.find { it.model.hostName == host.second } ?: continue
                val connection = IgnitionNodeConnector(node, host.first, canvas).apply {
                    to = outboundNode
                }
                node.connections[host.first] = connection
                connections.add(connection)
            }
        }

        return connections
    }

    @Serializable
    private data class IgnitionVersionsCache(
        val topTagName: String,
        val topTagLastUpdated: String,
        val versions: List<String>,
    )

    private val cacheFile: Path = Path(System.getProperty("user.home"), ".kindling", "ignition-versions.json")
    private val cacheJson = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private fun loadVersionsCache(): IgnitionVersionsCache? = runCatching {
        if (!cacheFile.exists()) null else cacheJson.decodeFromString<IgnitionVersionsCache>(cacheFile.readText())
    }.getOrNull()

    private fun saveVersionsCache(cache: IgnitionVersionsCache) {
        runCatching {
            cacheFile.parent.createDirectories()
            cacheFile.writeText(cacheJson.encodeToString(cache))
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun parseTopTag(body: ByteArray): Pair<String, String>? {
        val results = Json.decodeFromStream<JsonObject>(body.inputStream())["results"]!!.jsonArray
        val first = results.firstOrNull()?.jsonObject ?: return null
        val name = first["name"]?.jsonPrimitive?.content ?: return null
        val lastUpdated = first["last_updated"]?.jsonPrimitive?.content ?: return null
        return name to lastUpdated
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun parseAllVersions(body: ByteArray): List<String> {
        val results = Json.decodeFromStream<JsonObject>(body.inputStream())["results"]!!.jsonArray
        return results
            .mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
            .filterNot { it.length <= 4 }
            .sortedWith(IgnitionVersionComparator.reversed())
    }

    val ignitionImageVersions: Deferred<List<String>> by lazy {
        CoroutineScope(Dispatchers.IO).async {
            val cached = loadVersionsCache()
            runCatching {
                HttpClient.newHttpClient().use { client ->
                    val peekResp = client.send(
                        HttpRequest.newBuilder().GET().uri(URI.create(PEEK_URL)).build(),
                        HttpResponse.BodyHandlers.ofByteArray(),
                    )
                    val topTag = parseTopTag(peekResp.body())

                    if (cached != null && topTag != null &&
                        topTag.first == cached.topTagName &&
                        topTag.second == cached.topTagLastUpdated
                    ) {
                        return@use cached.versions
                    }

                    val fullResp = client.send(
                        HttpRequest.newBuilder().GET().uri(URI.create(FULL_URL)).build(),
                        HttpResponse.BodyHandlers.ofByteArray(),
                    )
                    val versions = parseAllVersions(fullResp.body())
                    if (topTag != null) {
                        saveVersionsCache(IgnitionVersionsCache(topTag.first, topTag.second, versions))
                    }
                    versions
                }
            }.getOrElse {
                it.printStackTrace()
                cached?.versions ?: fallbackVersionList
            }
        }
    }

    private val fallbackVersionList = listOf(
        "nightly",
        "latest",
        "8.3.1",
        "8.3.0",
        "8.1.47",
        "8.1.46",
        "8.1.46-rc1",
        "8.1.45",
        "8.1.45-rc1",
        "8.1.44",
        "8.1.44-rc1",
        "8.1.43",
        "8.1.43-rc1",
        "8.1.42",
        "8.1.42-rc1",
        "8.1.41",
        "8.1.40-rc1",
        "8.1.39",
        "8.1.38",
        "8.1.37",
        "8.1.36",
        "8.1.35",
        "8.1.33",
        "8.1.32",
        "8.1.31",
        "8.1.30",
        "8.1.28",
        "8.1.27",
        "8.1.26",
        "8.1.25",
        "8.1.24",
        "8.1.23",
        "8.1.22",
        "8.1.21",
        "8.1.20",
        "8.1.19",
        "8.1.18",
        "8.1.17",
        "8.1.16",
        "8.1.15",
        "8.1.14",
        "8.1.13",
        "8.1.12",
        "8.1.11",
        "8.1.11-rc2",
        "8.1.10",
        "8.1.9",
        "8.1.8",
        "8.1.7",
        "8.1.5",
        "8.1.4",
        "8.1.3",
        "8.1.2",
        "8.1.1",
        "8.1.0",
        "8.1.0-rc2",
    )
}
