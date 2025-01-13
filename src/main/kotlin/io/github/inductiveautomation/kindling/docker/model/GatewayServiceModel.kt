package io.github.inductiveautomation.kindling.docker.model

import io.github.inductiveautomation.kindling.docker.ui.GatewayServiceFlavor
import kotlin.random.Random
import kotlin.random.nextInt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("IgnitionGatewayService")
class GatewayServiceModel(
    override var image: String,
    override var hostName: String? = null,
    override var containerName: String = "Ignition-${Random.nextInt(1..10000)}",
    override val ports: MutableList<PortMapping> = mutableListOf(),
    override val environment: MutableSet<String> = mutableSetOf(),
    override val commands: MutableSet<CliArgument> = mutableSetOf(),
    override val volumes: MutableMap<String, String> = mutableMapOf(),
) : DockerServiceModel {
    @Transient
    var flavor: GatewayServiceFlavor = GatewayServiceFlavor.valueOf(image.substringBefore("/").uppercase())
        set(value) {
            field = value
            updateImage()
        }

    @Transient
    private var version: String = image.substringAfter(":")
        set(value) {
            field = value
            updateImage()
        }

    private fun updateImage() {
        image = "${flavor.serialName}/ignition:$version"
    }

    companion object {
        const val DEFAULT_IMAGE = "inductiveautomation/ignition:LATEST"
    }
}