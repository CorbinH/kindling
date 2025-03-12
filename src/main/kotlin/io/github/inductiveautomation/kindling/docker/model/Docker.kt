package io.github.inductiveautomation.kindling.docker.model

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
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


object Docker {
    private const val DOCKER_URL = "https://hub.docker.com/v2/repositories/inductiveautomation/ignition/tags?page_size=1000&page=1&ordering=last_updated"

    @OptIn(ExperimentalSerializationApi::class)
    val ignitionImageVersions: Deferred<List<String>> by lazy {
        CoroutineScope(Dispatchers.IO).async {
            runCatching {
                val client = HttpClient.newHttpClient()
                val req = HttpRequest.newBuilder().GET().uri(URI.create(DOCKER_URL)).build()
                val response = client.send(req, HttpResponse.BodyHandlers.ofInputStream())
                val jsonData = Json.decodeFromStream<JsonObject>(response.body())

                val l = jsonData["results"]!!.jsonArray

                val versions = l.mapNotNull {
                    it.jsonObject["name"]?.jsonPrimitive?.content
                }.toMutableList()

                versions.removeAll { it.length <= 4 }

                versions.sortedWith(IgnitionVersionComparator.reversed())
            }.getOrElse {
                fallbackVersionList
            }
        }
    }

    private val fallbackVersionList = listOf(
        "nightly",
        "latest",
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

enum class GatewayServiceFlavor {
    KCOLLINS,
    INDUCTIVEAUTOMATION,
    ;

    val serialName: String = name.lowercase()
}

object IgnitionVersionComparator : Comparator<String> {
    override fun compare(o1: String?, o2: String?): Int {
        if (o1 === o2) return 0
        if (o1 == null) return -1
        if (o2 == null) return 1

        // Nightly is greater than anything else
        if (o1.equals("NIGHTLY", true)) return 100
        if (o2.equals("NIGHTLY", true)) return -100

        // Latest is also greater than anything else
        if (o1.equals("LATEST", true)) return 99
        if (o2.equals("LATEST", true)) return -99


        val o1Split = o1.split(".", "-")
        val v1 = when(o1Split.size) {
            3 -> {
                val m = o1Split.map { it.toInt() }.toMutableList()
                m.add(1000)
                m
            }
            4 -> {
                o1Split.mapIndexed { index, value ->
                    if (index == 3) {
                        value.last().digitToInt()
                    } else {
                        value.toInt()
                    }
                }
            }
            else -> error("Malformed version: $o1")
        }

        val o2Split = o2.split(".", "-")
        val v2 = when(o2Split.size) {
            3 -> {
                val m = o2Split.map { it.toInt() }.toMutableList()
                m.add(1000) // Non release candidate is greater than release candidate
                m
            }
            4 -> {
                o2Split.mapIndexed { index, value ->
                    if (index == 3) {
                        value.last().digitToInt()
                    } else {
                        value.toInt()
                    }
                }
            }
            else -> error("Malformed version: $o1")
        }

        for ((version1, version2) in v1.zip(v2)) {
            val result = version1.compareTo(version2)
            if (result != 0) return result
        }

        return 0
    }
}
