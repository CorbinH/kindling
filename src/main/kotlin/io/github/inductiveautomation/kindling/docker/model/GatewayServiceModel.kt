package io.github.inductiveautomation.kindling.docker.model

import io.github.inductiveautomation.kindling.docker.serializers.CommandLineArgumentListSerializer
import io.github.inductiveautomation.kindling.docker.serializers.EnvironmentVariableSerializer
import io.github.inductiveautomation.kindling.docker.serializers.PointAsStringSerializer
import java.awt.Point
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("IgnitionGatewayService")
class GatewayServiceModel(
    override var image: String,
    @SerialName("hostname")
    override var hostName: String? = null,
    @SerialName("container_name")
    override var containerName: String,
    override val ports: MutableList<PortMapping> = mutableListOf(),
    @Serializable(with = EnvironmentVariableSerializer::class)
    override val environment: MutableMap<String, String> = mutableMapOf(),
    @SerialName("command")
    @Serializable(with = CommandLineArgumentListSerializer::class)
    override val commands: MutableList<CliArgument> = mutableListOf(),
    override val volumes: MutableList<BindMount> = mutableListOf(),
    override val networks: MutableList<String> = mutableListOf(),
    override val labels: List<String> = emptyList(),
) : DockerServiceModel {
    @SerialName("x-canvas.location")
    @Serializable(with = PointAsStringSerializer::class)
    override var canvasLocation: Point? = null

    init {
        if (hostName == null) {
            hostName = containerName.filter { it in hostNameValidChars }
        }

        environment.putAll(DEFAULT_VARIABLES)
    }

    @Transient
    var flavor: GatewayServiceFlavor = GatewayServiceFlavor.valueOf(image.substringBefore("/").uppercase())
        set(value) {
            field = value
            updateImage()
        }

    @Transient
    var version: String = image.substringAfter(":")
        set(value) {
            field = value
            updateImage()
        }

    private fun updateImage() {
        image = "${flavor.serialName}/ignition:$version"
    }

    companion object {
        const val DEFAULT_IMAGE = "inductiveautomation/ignition:latest"
        private val hostNameValidChars = ('A'..'Z') + ('0'..'9') + ('a'..'z') + '-'
        private val DEFAULT_VARIABLES = mapOf(
            StaticDefinition.ACCEPT_IGNITION_EULA.name to "Y",
            StaticDefinition.GATEWAY_ADMIN_USERNAME.name to "admin",
            StaticDefinition.GATEWAY_ADMIN_PASSWORD.name to "password",
            StaticDefinition.IGNITION_EDITION.name to "standard",
        )
    }
}