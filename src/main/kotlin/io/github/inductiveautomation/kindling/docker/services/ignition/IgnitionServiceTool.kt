package io.github.inductiveautomation.kindling.docker.services.ignition

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.docker.DockerDraftPanel
import io.github.inductiveautomation.kindling.docker.services.DockerServiceTool
import io.github.inductiveautomation.kindling.docker.services.ignition.model.IgnitionServiceModel
import io.github.inductiveautomation.kindling.docker.services.ignition.model.IgnitionVersionComparator
import io.github.inductiveautomation.kindling.docker.services.model.DefaultDockerServiceModel
import io.github.inductiveautomation.kindling.docker.services.model.DockerEnvironmentVariableDefinition.Companion.getConnectionVariableIndex
import io.github.inductiveautomation.kindling.docker.services.model.DockerServiceModel
import io.github.inductiveautomation.kindling.docker.services.model.PortMapping
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.serialization.ExperimentalSerializationApi
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
import kotlin.collections.find

object IgnitionServiceTool : DockerServiceTool {
    override val icon = FlatSVGIcon("icons/Logo-Ignition-Check.svg")
    override val name = "Ignition Gateway Node"
    override val defaultImage = "inductiveautomation/ignition:latest"

    private const val DOCKER_URL =
        "https://hub.docker.com/v2/repositories/inductiveautomation/ignition/tags?page_size=1000&page=1&ordering=last_updated"

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

    @OptIn(ExperimentalSerializationApi::class)
    val ignitionImageVersions: Deferred<List<String>> by lazy {
        CoroutineScope(Dispatchers.IO).async {
            runCatching {
                HttpClient.newHttpClient().use { client ->
                    val req = HttpRequest.newBuilder().GET().uri(URI.create(DOCKER_URL)).build()
                    val response = client.send(req, HttpResponse.BodyHandlers.ofInputStream())
                    val jsonData = Json.decodeFromStream<JsonObject>(response.body())

                    val l = jsonData["results"]!!.jsonArray

                    val versions = l.mapNotNull {
                        it.jsonObject["name"]?.jsonPrimitive?.content
                    }.toMutableList()

                    versions.removeAll { it.length <= 4 }

                    versions.sortedWith(IgnitionVersionComparator.reversed())
                }
            }.getOrElse {
                println("Error occured:")
                it.printStackTrace()
                fallbackVersionList
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
