package io.github.inductiveautomation.kindling.docker.model

import io.github.inductiveautomation.kindling.docker.serializers.CommandLineArgumentListSerializer
import kotlin.random.Random
import kotlin.random.nextInt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("IgnitionGatewayService")
class GatewayServiceModel(
    override var image: String = DEFAULT_IMAGE,
    override var hostName: String? = null,
    override var containerName: String = "Ignition-${Random.nextInt(1..10000)}",
    override val ports: MutableList<PortMapping> = mutableListOf(),
    override val environment: MutableMap<String, String> = mutableMapOf(),
    @Serializable(with = CommandLineArgumentListSerializer::class)
    override val commands: MutableList<CliArgument> = mutableListOf(),
    override val volumes: MutableList<DockerVolumeServiceBinding> = mutableListOf(),
    override val networks: MutableList<DockerNetwork> = mutableListOf(),
) : DockerServiceModel {

    init {
        if (hostName == null) {
            hostName = containerName.filter { it in hostNameValidChars }
        }
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
        private const val DEFAULT_IMAGE = "inductiveautomation/ignition:latest"
        private val hostNameValidChars = ('A'..'Z') + ('0'..'9') + ('a'..'z') + '-'
    }
}