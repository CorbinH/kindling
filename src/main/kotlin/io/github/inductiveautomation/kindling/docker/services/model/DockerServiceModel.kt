package io.github.inductiveautomation.kindling.docker.services.model

import io.github.inductiveautomation.kindling.docker.compose.model.Build
import io.github.inductiveautomation.kindling.docker.compose.model.DependsOn
import io.github.inductiveautomation.kindling.docker.compose.model.Deploy
import io.github.inductiveautomation.kindling.docker.compose.model.Restart
import io.github.inductiveautomation.kindling.docker.volumes.model.BindMount
import io.github.inductiveautomation.kindling.docker.serializers.CommandLineArgumentListSerializer
import io.github.inductiveautomation.kindling.docker.serializers.DependsOnConfigSerializer
import io.github.inductiveautomation.kindling.docker.serializers.MapOrListSerializer
import io.github.inductiveautomation.kindling.docker.serializers.PointAsStringSerializer
import io.github.inductiveautomation.kindling.docker.services.ignition.model.CliArgument
import io.github.inductiveautomation.kindling.utils.add
import io.github.inductiveautomation.kindling.utils.getAll
import io.github.inductiveautomation.kindling.utils.remove
import java.awt.Point
import java.util.EventListener
import javax.swing.event.EventListenerList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

interface DockerServiceModel {
    var image: String
    var hostName: String?
    var containerName: String
    val ports: MutableList<PortMapping>
    val environment: MutableMap<String, String>
    val commands: MutableList<CliArgument>
    val volumes: MutableList<BindMount>
    val networks: MutableMap<String, ServiceNetworkConnection>
    val labels: MutableMap<String, String>

    // Other Properties:
    var dependsOn: MutableMap<String, DependsOn>
    val envFile: MutableList<String>
    var attach: Boolean
    var build: Build
    var deploy: Deploy
    val entrypoint: MutableList<String>
    var restart: Restart

    var pullPolicy: String?
    var readOnly: String
    var user: String?
    val capAdd: MutableList<String>
    val capDrop: MutableList<String>
    val securityOpt: MutableList<String>
    var cGroup: String?
    var pid: String?

    var canvasLocation: Point?
    val defaultModel: DefaultDockerServiceModel

    fun addServiceModelChangeListener(l: ServiceModelChangeListener)
    fun removeServiceModelChangeListener(l: ServiceModelChangeListener)
    fun fireServiceModelChangedEvent()
}

@Serializable
class DefaultDockerServiceModel(
    override var image: String,
    @SerialName("hostname")
    override var hostName: String? = null,
    @SerialName("container_name")
    override var containerName: String = "",
    override val ports: MutableList<PortMapping> = mutableListOf(),
    @Serializable(with = MapOrListSerializer::class)
    override val environment: MutableMap<String, String> = mutableMapOf(),
    @SerialName("command")
    @Serializable(with = CommandLineArgumentListSerializer::class)
    override val commands: MutableList<CliArgument> = mutableListOf(),
    override val volumes: MutableList<BindMount> = mutableListOf(),
    override val networks: MutableMap<String, ServiceNetworkConnection> = mutableMapOf(),
    @Serializable(with = MapOrListSerializer::class)
    override val labels: MutableMap<String, String> = mutableMapOf(),
    // Other Properties:
    @SerialName("depends_on")
    @Serializable(with = DependsOnConfigSerializer::class)
    override var dependsOn: MutableMap<String, DependsOn> = mutableMapOf(),
    @SerialName("env_file")
    override val envFile: MutableList<String> = mutableListOf(),
    override var attach: Boolean = true,
    override var build: Build = Build(),
    override var deploy: Deploy = Deploy(),
    override val entrypoint: MutableList<String> = mutableListOf(),
    override var restart: Restart = Restart.NO,
    @SerialName("pull_policy")
    override var pullPolicy: String? = null,
    @SerialName("read_only")
    override var readOnly: String = "no",
    override var user: String? = null,
    @SerialName("cap_add")
    override val capAdd: MutableList<String> = mutableListOf(),
    @SerialName("cap_drop")
    override val capDrop: MutableList<String> = mutableListOf(),
    @SerialName("security_opt")
    override val securityOpt: MutableList<String> = mutableListOf(),
    @SerialName("cgroup")
    override var cGroup: String? = null,
    override var pid: String? = null,
) : DockerServiceModel {
    @SerialName("x-canvas.location")
    @Serializable(with = PointAsStringSerializer::class)
    override var canvasLocation: Point? = null

    @Transient
    private val listenerList = EventListenerList()

    @Transient
    override val defaultModel = this

    override fun addServiceModelChangeListener(l: ServiceModelChangeListener) = listenerList.add(l)
    override fun removeServiceModelChangeListener(l: ServiceModelChangeListener) = listenerList.remove(l)

    override fun fireServiceModelChangedEvent() {
        listenerList.getAll<ServiceModelChangeListener>().forEach(ServiceModelChangeListener::onServiceModelChanged)
    }
}

fun interface ServiceModelChangeListener : EventListener {
    fun onServiceModelChanged()
}
