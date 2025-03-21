package io.github.inductiveautomation.kindling.docker.model

import io.github.inductiveautomation.kindling.docker.serializers.CommandLineArgumentListSerializer
import io.github.inductiveautomation.kindling.docker.serializers.DockerServiceModelSerializer
import io.github.inductiveautomation.kindling.docker.serializers.EnvironmentVariableListSerializer
import io.github.inductiveautomation.kindling.docker.serializers.PointAsStringSerializer
import java.awt.Point
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KeepGeneratedSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = DockerServiceModelSerializer::class)
@KeepGeneratedSerializer
open class DockerServiceModel(
    var image: String,
    @SerialName("hostname")
    var hostName: String? = null,
    @SerialName("container_name")
    var containerName: String = "",
    val ports: MutableList<PortMapping> = mutableListOf(),
    @SerialName("environment")
    @Serializable(with = EnvironmentVariableListSerializer::class)
    var envList: List<EnvironmentVariable> = emptyList(),
    @SerialName("command")
    @Serializable(with = CommandLineArgumentListSerializer::class)
    val commands: MutableList<CliArgument> = mutableListOf(),
    val volumes: MutableList<BindMount> = mutableListOf(),
    val networks: MutableList<String> = mutableListOf(),
    val labels: List<String> = emptyList(),
) {
    @Transient
    val environment: MutableMap<String, String> = envList.toMap().toMutableMap()

    @SerialName("x-canvas.location")
    @Serializable(with = PointAsStringSerializer::class)
    var canvasLocation: Point? = null

    companion object {
        const val DEFAULT_GENERIC_IMAGE = "kcollins/mssql:latest"
    }
}

fun interface ServiceModelChangeListener : java.util.EventListener {
    fun onServiceModelChanged()
}
