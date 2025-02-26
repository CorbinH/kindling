package io.github.inductiveautomation.kindling.docker.model

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class GatewayServiceFlavor {
    KCOLLINS,
    INDUCTIVEAUTOMATION,
    ;

    val serialName: String = name.lowercase()
}

const val DOCKER_URL = "https://hub.docker.com/v2/repositories/inductiveautomation/ignition/tags?page_size=1000&page=1&ordering=last_updated"

val ignitionImageVersions: Deferred<List<String>> by lazy {
    CoroutineScope(Dispatchers.IO).async {
        val client = HttpClient.newHttpClient()
        val req = HttpRequest.newBuilder().GET().uri(URI.create(DOCKER_URL)).build()
        val response = client.send(req, HttpResponse.BodyHandlers.ofString())
        val jsonData = Json.decodeFromString<JsonObject>(response.body())

        val l = jsonData.get("results")!!.jsonArray

        val versions = l.mapNotNull {
            it.jsonObject["name"]?.jsonPrimitive?.content
        }.toMutableList()

        versions.removeAll { it.length <= 4 }

        versions.sortedWith(IgnitionVersionComparator.reversed())
    }
}
