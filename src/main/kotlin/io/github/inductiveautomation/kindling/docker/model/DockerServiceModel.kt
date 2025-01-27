package io.github.inductiveautomation.kindling.docker.model

import io.github.inductiveautomation.kindling.docker.serializers.CommandLineArgumentListSerializer
import io.github.inductiveautomation.kindling.docker.serializers.DockerNetworkListStringSerializer
import io.github.inductiveautomation.kindling.docker.serializers.DockerVolumeBindingListSerializer
import io.github.inductiveautomation.kindling.utils.add
import io.github.inductiveautomation.kindling.utils.getAll
import javax.swing.event.EventListenerList
import kotlin.random.Random
import kotlin.random.nextInt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed interface DockerServiceModel {
    var image: String

    var hostName: String?

    @SerialName("container_name")
    var containerName: String

    val ports: List<PortMapping>

    val environment: List<String>

    @Serializable(with = CommandLineArgumentListSerializer::class)
    val commands: List<CliArgument>

    val volumes: List<DockerVolumeServiceBinding>

    val networks: List<DockerNetwork>

    fun addServiceModelChangeListener(l: ServiceModelChangeListener)

    fun fireServiceModelChangedEvent()
}

fun interface ServiceModelChangeListener : java.util.EventListener {
    fun onServiceModelChanged()
}

@Serializable
@SerialName("GenericDockerService")
class DefaultDockerServiceModel(
    override var image: String,
    override var hostName: String? = null,
    @SerialName("container_name")
    override var containerName: String = "Container-${Random.nextInt(0..100000)}",
    override val ports: MutableList<PortMapping> = mutableListOf(),
    override val environment: MutableList<String> = mutableListOf(),
    @Serializable(with = CommandLineArgumentListSerializer::class)
    override val commands: MutableList<CliArgument> = mutableListOf(),
    @Serializable(with = DockerVolumeBindingListSerializer::class)
    override val volumes: MutableList<DockerVolumeServiceBinding> = mutableListOf(),
    @Serializable(with = DockerNetworkListStringSerializer::class)
    override val networks: MutableList<DockerNetwork> = mutableListOf(),
) : DockerServiceModel {
    @Transient
    private val listeners = EventListenerList()

    override fun addServiceModelChangeListener(l: ServiceModelChangeListener) {
        listeners.add(l)
    }

    override fun fireServiceModelChangedEvent() {
        listeners.getAll<ServiceModelChangeListener>().forEach(ServiceModelChangeListener::onServiceModelChanged)
    }

    companion object {
        const val DEFAULT_GENERIC_IMAGE = "kcollin/mssql:LATEST"
    }
}