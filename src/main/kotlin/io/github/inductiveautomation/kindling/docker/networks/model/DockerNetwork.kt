package io.github.inductiveautomation.kindling.docker.networks.model

import io.github.inductiveautomation.kindling.docker.serializers.MapOrListSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DockerNetwork(
    var name: String? = null,
    var driver: String? = null,
    var attachable: Boolean = false,
    @SerialName("driver_opts")
    val driverOpts: MutableMap<String, String> = mutableMapOf(),
    @SerialName("enable_ipv4")
    var enableIpv4: Boolean = true,
    @SerialName("enable_ipv6")
    var enableIpv6: Boolean = false,
    var external: Boolean = false,
    var internal: Boolean = false,
    @Serializable(with = MapOrListSerializer::class)
    val labels: MutableMap<String, String> = mutableMapOf(),
    val ipam: Ipam = Ipam(),
) {
    @Serializable
    data class Ipam(
        var driver: String? = null,
        val config: MutableList<IpamConfig> = mutableListOf(),
        val options: MutableMap<String, String> = mutableMapOf(),
    ) {
        @Serializable
        data class IpamConfig(
            var subnet: String,
            @SerialName("ip_range")
            var ipRange: String,
            var gateway: String,
            @SerialName("aux_addresses")
            val auxAddresses: MutableMap<String, String> = mutableMapOf(),
        )
    }
}