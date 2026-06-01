package io.github.inductiveautomation.kindling.docker.services.model

import io.github.inductiveautomation.kindling.docker.serializers.ComplexPortMappingSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = ComplexPortMappingSerializer::class)
@KeepGeneratedSerializer
data class PortMapping(
    var published: String,
    var target: String,
    var name: String? = null,
    @SerialName("host_ip")
    var hostIp: String = DEFAULT_HOST_IP,
    var protocol: String = DEFAULT_PROTOCOL,
    @SerialName("app_protocol")
    var appProtocol: String? = null,
    var mode: String = DEFAULT_MODE,
) {
    companion object {
        const val DEFAULT_HOST_IP = "0.0.0.0"
        const val DEFAULT_PROTOCOL = "tcp"
        const val DEFAULT_MODE = "ingress"
    }
}
