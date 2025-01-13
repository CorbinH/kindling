package io.github.inductiveautomation.kindling.docker.model

import io.github.inductiveautomation.kindling.docker.serializers.CommandLineArgumentListSerializer
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

    val ports: List<PortMapping>

    val environment: MutableSet<String>

    @Serializable(with = CommandLineArgumentListSerializer::class)
    val commands: MutableSet<CliArgument>

    val volumes: MutableMap<String, String>
}

@Serializable
@SerialName("GenericDockerService")
class DefaultDockerServiceModel(
    override var image: String,
    override var hostName: String? = null,
    override var containerName: String = "Container-${Random.nextInt(0..100000)}",
    override val ports: MutableList<PortMapping> = mutableListOf(),
    override val environment: MutableSet<String> = mutableSetOf(),
    override val commands: MutableSet<CliArgument> = mutableSetOf(),
    override val volumes: MutableMap<String, String> = mutableMapOf(),
) : DockerServiceModel {
    companion object {
        const val DEFAULT_GENERIC_IMAGE = "kcollin/mssql:LATEST"
    }
}