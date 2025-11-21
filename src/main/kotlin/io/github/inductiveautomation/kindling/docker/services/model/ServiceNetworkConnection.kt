package io.github.inductiveautomation.kindling.docker.services.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ServiceNetworkConnection(
    @SerialName("ipv4_address")
    var ipv4Address: String? = null,
    @SerialName("ipv6_address")
    var ipv6Address: String? = null,
    @SerialName("link_local_ips")
    val linkLocalIPs: MutableList<String> = mutableListOf(),
    @SerialName("mac_address")
    var macAddress: String? = null,
    @SerialName("driver_opts")
    val driverOpts: MutableMap<String, String> = mutableMapOf(),
    @SerialName("gw_priority")
    var gwPriority: Int = 0,
    var priority: Int = 0,
)