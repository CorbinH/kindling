package io.github.inductiveautomation.kindling.docker.model

import io.github.inductiveautomation.kindling.docker.model.compose.Build
import io.github.inductiveautomation.kindling.docker.model.compose.DependsOn
import io.github.inductiveautomation.kindling.docker.model.compose.Deploy
import io.github.inductiveautomation.kindling.docker.serializers.CommandLineArgumentListSerializer
import io.github.inductiveautomation.kindling.docker.serializers.DependsOnConfigSerializer
import io.github.inductiveautomation.kindling.docker.serializers.EnvironmentVariableSerializer
import io.github.inductiveautomation.kindling.docker.serializers.PointAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.awt.Point
import java.util.EventListener

@Serializable
open class DockerServiceModel(
    var image: String,
    @SerialName("hostname")
    var hostName: String? = null,
    @SerialName("container_name")
    var containerName: String = "",
    val ports: MutableList<PortMapping> = mutableListOf(),
    @Serializable(with = EnvironmentVariableSerializer::class)
    val environment: MutableMap<String, String> = mutableMapOf(),
    @SerialName("command")
    @Serializable(with = CommandLineArgumentListSerializer::class)
    val commands: MutableList<CliArgument> = mutableListOf(),
    val volumes: MutableList<BindMount> = mutableListOf(),
    val networks: MutableList<String> = mutableListOf(),
    val labels: List<String> = emptyList(),
    // Other Properties:
    @SerialName("depends_on")
    @Serializable(with=DependsOnConfigSerializer::class)
    var dependsOn: MutableMap<String, DependsOn> = mutableMapOf(),
    var envFile: String? = null,
    var attach: Boolean = true,
    var build: Build? = null,
    var deploy: Deploy? = null,
) {
    @SerialName("x-canvas.location")
    @Serializable(with = PointAsStringSerializer::class)
    var canvasLocation: Point? = null

    companion object {
        const val DEFAULT_GENERIC_IMAGE = "kcollins/mssql:latest"
    }
}

fun interface ServiceModelChangeListener : EventListener {
    fun onServiceModelChanged()
}
