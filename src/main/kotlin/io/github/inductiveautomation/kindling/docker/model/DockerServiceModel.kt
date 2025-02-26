package io.github.inductiveautomation.kindling.docker.model

import io.github.inductiveautomation.kindling.docker.serializers.CommandLineArgumentListSerializer
import io.github.inductiveautomation.kindling.docker.serializers.DockerNetworkListStringSerializer
import io.github.inductiveautomation.kindling.docker.serializers.DockerVolumeBindingListSerializer
import kotlin.random.Random
import kotlin.random.nextInt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface DockerServiceModel {
    var image: String

    var hostName: String?

    @SerialName("container_name")
    var containerName: String

    val ports: MutableList<PortMapping>

    val environment: MutableMap<String, String>

    @Serializable(with = CommandLineArgumentListSerializer::class)
    val commands: MutableList<CliArgument>

    val volumes: MutableList<DockerVolumeServiceBinding>

    val networks: MutableList<DockerNetwork>
}

fun interface ServiceModelChangeListener : java.util.EventListener {
    fun onServiceModelChanged()
}

@Serializable
@SerialName("GenericDockerService")
class DefaultDockerServiceModel(
    override var image: String = DEFAULT_GENERIC_IMAGE,
    override var hostName: String? = null,
    @SerialName("container_name")
    override var containerName: String = "Container-${Random.nextInt(0..100000)}",
    override val ports: MutableList<PortMapping> = mutableListOf(),
    override val environment: MutableMap<String, String> = mutableMapOf(),
    @Serializable(with = CommandLineArgumentListSerializer::class)
    override val commands: MutableList<CliArgument> = mutableListOf(),
    @Serializable(with = DockerVolumeBindingListSerializer::class)
    override val volumes: MutableList<DockerVolumeServiceBinding> = mutableListOf(),
    @Serializable(with = DockerNetworkListStringSerializer::class)
    override val networks: MutableList<DockerNetwork> = mutableListOf(),
) : DockerServiceModel {
    companion object {
        private const val DEFAULT_GENERIC_IMAGE = "kcollin/mssql:LATEST"
    }
}