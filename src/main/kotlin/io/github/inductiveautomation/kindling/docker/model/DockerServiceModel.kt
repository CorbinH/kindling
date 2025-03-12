package io.github.inductiveautomation.kindling.docker.model

import io.github.inductiveautomation.kindling.docker.serializers.CommandLineArgumentListSerializer
import io.github.inductiveautomation.kindling.docker.serializers.EnvironmentVariableSerializer
import io.github.inductiveautomation.kindling.docker.serializers.PointAsStringSerializer
import java.awt.Point
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface DockerServiceModel {
    var image: String

    @SerialName("hostname")
    var hostName: String?

    @SerialName("container_name")
    var containerName: String

    val ports: MutableList<PortMapping>

    @Serializable(with = EnvironmentVariableSerializer::class)
    val environment: MutableMap<String, String>

    @SerialName("command")
    @Serializable(with = CommandLineArgumentListSerializer::class)
    val commands: MutableList<CliArgument>

    val volumes: MutableList<DockerVolumeServiceBinding>

    val networks: MutableList<String>

    @SerialName("x-canvas.location")
    var canvasLocation: Point?
}

fun interface ServiceModelChangeListener : java.util.EventListener {
    fun onServiceModelChanged()
}

@Serializable
@SerialName("GenericDockerService")
class DefaultDockerServiceModel(
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
    override val volumes: MutableList<DockerVolumeServiceBinding> = mutableListOf(),
    override val networks: MutableList<String> = mutableListOf(),
) : DockerServiceModel {
    @SerialName("x-canvas.location")
    @Serializable(with = PointAsStringSerializer::class)
    override var canvasLocation: Point? = null

    companion object {
        const val DEFAULT_GENERIC_IMAGE = "kcollins/mssql:latest"
    }
}