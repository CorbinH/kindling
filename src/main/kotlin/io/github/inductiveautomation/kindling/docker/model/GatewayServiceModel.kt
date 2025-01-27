package io.github.inductiveautomation.kindling.docker.model

import io.github.inductiveautomation.kindling.docker.serializers.CommandLineArgumentListSerializer
import io.github.inductiveautomation.kindling.docker.ui.GatewayServiceFlavor
import io.github.inductiveautomation.kindling.utils.add
import io.github.inductiveautomation.kindling.utils.getAll
import javax.swing.event.EventListenerList
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
    override val environment: MutableList<String> = mutableListOf(),
    @Serializable(with = CommandLineArgumentListSerializer::class)
    override val commands: MutableList<CliArgument> = mutableListOf(),
    override val volumes: MutableList<DockerVolumeServiceBinding> = mutableListOf(),
    override val networks: List<DockerNetwork> = mutableListOf(),
) : DockerServiceModel {
    @Transient
    private val listeners = EventListenerList()

    override fun addServiceModelChangeListener(l: ServiceModelChangeListener) {
        listeners.add(l)
    }

    override fun fireServiceModelChangedEvent() {
        listeners.getAll<ServiceModelChangeListener>().forEach(ServiceModelChangeListener::onServiceModelChanged)
    }

    @Transient
    val outgoingConnections: MutableList<GatewayServiceModel> = mutableListOf()

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