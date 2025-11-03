package io.github.inductiveautomation.kindling.docker.model

import io.github.inductiveautomation.kindling.docker.model.compose.Build
import io.github.inductiveautomation.kindling.docker.model.compose.DependsOn
import io.github.inductiveautomation.kindling.docker.model.compose.Deploy
import io.github.inductiveautomation.kindling.docker.model.compose.Restart
import io.github.inductiveautomation.kindling.docker.serializers.CommandLineArgumentListSerializer
import io.github.inductiveautomation.kindling.docker.serializers.DependsOnConfigSerializer
import io.github.inductiveautomation.kindling.docker.serializers.MapOrListSerializer
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
    @Serializable(with = MapOrListSerializer::class)
    val environment: MutableMap<String, String> = mutableMapOf(),
    @SerialName("command")
    @Serializable(with = CommandLineArgumentListSerializer::class)
    val commands: MutableList<CliArgument> = mutableListOf(),
    val volumes: MutableList<BindMount> = mutableListOf(),
    val networks: MutableList<String> = mutableListOf(),
    @Serializable(with = MapOrListSerializer::class)
    val labels: MutableMap<String, String> = mutableMapOf(),
    // Other Properties:
    @SerialName("depends_on")
    @Serializable(with = DependsOnConfigSerializer::class)
    var dependsOn: MutableMap<String, DependsOn> = mutableMapOf(),
    @SerialName("env_file")
    val envFile: MutableList<String> = mutableListOf(),
    var attach: Boolean = true,
    var build: Build = Build(),
    var deploy: Deploy = Deploy(),
    val entrypoint: MutableList<String> = mutableListOf(),
    var restart: Restart = Restart.NO,
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
