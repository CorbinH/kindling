package io.github.inductiveautomation.kindling.docker.services.mssql

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.docker.DockerDraftPanel
import io.github.inductiveautomation.kindling.docker.services.DockerServiceTool
import io.github.inductiveautomation.kindling.docker.services.model.DefaultDockerServiceModel
import io.github.inductiveautomation.kindling.docker.services.model.DockerServiceModel
import io.github.inductiveautomation.kindling.docker.services.mssql.model.MSSQLServiceModel
import io.github.inductiveautomation.kindling.docker.services.mssql.model.MSSQLServiceModel.Companion.DEFAULT_MSSQL_IMAGE
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
import javax.swing.Icon

object MssqlServiceTool : DockerServiceTool {
    override val icon: Icon = FlatSVGIcon("icons/microsoft-sql-server.svg")
    override val name: String = "MSSQL Server"
    override val defaultImage: String = "kcollins/mssql:latest"

    private const val MSSQL_DOCKER_URL =
        "https://hub.docker.com/v2/repositories/kcollins/mssql/tags?page_size=1000&page=1&ordering=last_updated"

    context(panel: DockerDraftPanel)
    override fun createModel(): MSSQLServiceModel = modelFromDefault(
        DefaultDockerServiceModel(
            image = DEFAULT_MSSQL_IMAGE,
            containerName = "MSSQL-${panel.nodeIdManager.generateID()}",
        ),
    )

    context(panel: DockerDraftPanel)
    override fun createNode(model: DockerServiceModel): MSSQLServiceNode {
        require(model is MSSQLServiceModel) {
            "Model ${model::class.java.name} is not an MSSQL Service Model"
        }
        return MSSQLServiceNode(model, panel.volumes, panel.networks.keys.toList())
    }

    override fun isValidCandidate(model: DefaultDockerServiceModel): Boolean = model.image.startsWith("kcollins/mssql")

    override fun modelFromDefault(model: DefaultDockerServiceModel) = MSSQLServiceModel(model)

    @OptIn(ExperimentalSerializationApi::class)
    val mssqlImageVersions: Deferred<List<String>> by lazy {
        CoroutineScope(Dispatchers.IO).async {
            runCatching {
                HttpClient.newHttpClient().use { client ->
                    val req = HttpRequest.newBuilder().GET().uri(URI.create(MSSQL_DOCKER_URL)).build()
                    val response = client.send(req, HttpResponse.BodyHandlers.ofInputStream())
                    val jsonData = Json.decodeFromStream<JsonObject>(response.body())

                    val l = jsonData["results"]!!.jsonArray

                    val versions = l.mapNotNull {
                        it.jsonObject["name"]?.jsonPrimitive?.content
                    }.toList()

                    versions
                }
            }.getOrElse {
                mssqlFallbackVersionList
            }
        }
    }

    private val mssqlFallbackVersionList = listOf(
        "latest",
        "2022-latest",
        "2022",
        "2019-latest",
        "2019",
        "2017-latest",
        "2017",
    )
}
