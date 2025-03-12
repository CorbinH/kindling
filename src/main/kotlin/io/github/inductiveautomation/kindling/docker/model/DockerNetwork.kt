package io.github.inductiveautomation.kindling.docker.model

import io.github.inductiveautomation.kindling.docker.serializers.DockerNetworkSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = DockerNetworkSerializer::class)
data class DockerNetwork(
    var name: String,
    var driver: String? = null,
    @SerialName("driver_opts")
    val driverOpts: MutableMap<String, String> = mutableMapOf(),
    var enableIpv6: Boolean = false,
    var external: Boolean = false,
    var internal: Boolean = false,
    val labels: MutableList<String> = mutableListOf(),
)