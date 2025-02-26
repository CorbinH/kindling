package io.github.inductiveautomation.kindling.docker

import com.charleskorn.kaml.MultiLineStringStyle
import com.charleskorn.kaml.SequenceStyle
import com.charleskorn.kaml.SingleLineStringStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.extras.components.FlatSplitPane
import io.github.inductiveautomation.kindling.core.Kindling
import io.github.inductiveautomation.kindling.core.Theme.Companion.theme
import io.github.inductiveautomation.kindling.core.Tool
import io.github.inductiveautomation.kindling.core.ToolPanel
import io.github.inductiveautomation.kindling.docker.model.DefaultDockerServiceModel
import io.github.inductiveautomation.kindling.docker.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.model.DockerVolume
import io.github.inductiveautomation.kindling.docker.model.GatewayServiceModel
import io.github.inductiveautomation.kindling.docker.ui.AbstractDockerServiceNode
import io.github.inductiveautomation.kindling.docker.ui.Canvas
import io.github.inductiveautomation.kindling.docker.ui.CanvasNodeList
import io.github.inductiveautomation.kindling.docker.ui.GatewayNodeConnector
import io.github.inductiveautomation.kindling.docker.ui.GatewayServiceNode
import io.github.inductiveautomation.kindling.docker.ui.GenericDockerServiceNode
import io.github.inductiveautomation.kindling.docker.ui.NetworksList
import io.github.inductiveautomation.kindling.docker.ui.NodeInitializer
import io.github.inductiveautomation.kindling.docker.ui.VolumesList
import io.github.inductiveautomation.kindling.utils.FileFilter
import io.github.inductiveautomation.kindling.utils.FlatScrollPane
import io.github.inductiveautomation.kindling.utils.HorizontalSplitPane
import io.github.inductiveautomation.kindling.utils.TrivialListDataListener
import io.github.inductiveautomation.kindling.utils.traverseChildren
import java.awt.EventQueue
import java.awt.Font
import java.awt.event.ContainerEvent
import java.awt.event.ContainerListener
import java.nio.file.Path
import javax.swing.JLabel
import javax.swing.JPanel
import kotlinx.serialization.encodeToString
import net.miginfocom.swing.MigLayout
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_YAML

@Suppress("unused")
class DockerDraftPanel : ToolPanel("ins 0, fill, hidemode 3") {
    override val icon = DockerTool.icon

    private val canvas = Canvas("Docker Drafting")

    private val optionsLabel = JLabel("Components").apply {
        font = font.deriveFont(Font.BOLD, 14F)
    }
    private val optionList = CanvasNodeList(
        listOf(
            NodeInitializer {
                GatewayServiceNode(
                    GatewayServiceModel(),
                    initialNetworkOptions = networks,
                    initialVolumeOptions = volumes,
                    parent = canvas,
                ).apply {
                    bindYamlPreview()
                    connectionObserver.observeConnection(this)
                }
            },
            NodeInitializer {
                GenericDockerServiceNode(
                    DefaultDockerServiceModel(""),
                    initialVolumeOptions = volumes,
                    initialNetworkOptions = networks,
                )
            }
        )
    )

    private val previewLabel = JLabel("YAML Preview").apply {
        font = font.deriveFont(Font.BOLD, 14F)
    }
    private val yamlPreview = RSyntaxTextArea().apply {
        theme = Kindling.Preferences.UI.Theme.currentValue
        syntaxEditingStyle = SYNTAX_STYLE_YAML
    }

    private var volumes: List<DockerVolume> = listOf(DockerVolume("db-data"), DockerVolume("backup-data"))
        set(value) {
            field = value
            services.forEach {
                it.volumeOptions = value
            }
        }

    private var networks = listOf(DockerNetwork("network1"), DockerNetwork("network2"))
        set(value) {
            field = value
            services.forEach {
                it.networkOptions = value
            }
        }

    private val volumesLabel = JLabel("Volumes").apply {
        font = font.deriveFont(Font.BOLD, 14F)
    }
    private val volumesList = VolumesList(volumes.toMutableList()).apply {
        volumesList.model.addListDataListener(
            TrivialListDataListener {
                val options = Array<DockerVolume>(volumesList.model.size) {
                    volumesList.model.getElementAt(it)
                }
                volumes = options.toList()
            }
        )
    }

    private val networksLabel = JLabel("Networks").apply {
        font = font.deriveFont(Font.BOLD, 14F)
    }
    private val networksList = NetworksList(networks.toMutableList()).apply {
        networksList.model.addListDataListener(
            TrivialListDataListener {
                val options = Array<DockerNetwork>(networksList.model.size) {
                    networksList.model.getElementAt(it)
                }
                networks = options.toList()
            }
        )
    }

    private val sidebar = JPanel(MigLayout("flowy, nogrid, fill")).apply {
        add(optionsLabel, "gapbottom 3")
        add(optionList, "gapbottom 10, wmin 300, growx")
        add(volumesLabel, "gapbottom 3")
        add(volumesList, "grow, gapbottom 3")
        add(networksLabel, "gapbottom 3")
        add(networksList, "grow, gapbottom 10")
        add(previewLabel, "gapbottom 3")
        add(FlatScrollPane(yamlPreview), "pushy, grow, wmin 300")
    }

    private val services: List<AbstractDockerServiceNode<*>>
        get() = canvas.traverseChildren(false).filterIsInstance<AbstractDockerServiceNode<*>>().toList()

    private val connectionObserver = ConnectionObserver()

    init {
        name = "Docker Draft Test"
        toolTipText = ""

        add(
            HorizontalSplitPane(
                left = canvas,
                right = sidebar,
                resizeWeight = 0.5,
                expandableSide = FlatSplitPane.ExpandableSide.left,
            ) {
                EventQueue.invokeLater {
                    dividerLocation = this@HorizontalSplitPane.size.width -
                            this@HorizontalSplitPane.insets.right -
                            rightComponent.minimumSize.width - dividerSize
                }
            }, "push, grow"
        )

        canvas.addContainerListener(
            object : ContainerListener {
                override fun componentAdded(e: ContainerEvent?) {
                    updatePreview()
                }

                override fun componentRemoved(e: ContainerEvent?) {
                    updatePreview()
                }
            }
        )

        addTestNodes()
        updatePreview()
    }

    private fun GatewayServiceNode.bindYamlPreview() {
        addServiceModelChangeListener {
            updatePreview()
        }
    }

    private fun updatePreview() {
        val yamlMap = mapOf(
            "services" to services.associate { it.model.containerName to it.model },
        )

        yamlPreview.text = runCatching {
            YAML.encodeToString(yamlMap)
        }.getOrElse { error ->
            error.stackTraceToString()
        }
    }

    private fun addTestNodes() {
        val testNode1 = GatewayServiceNode(initialVolumeOptions = volumes, initialNetworkOptions = networks, parent = canvas).apply {
            bindYamlPreview()
            connectionObserver.observeConnection(this)
        }
        val testNode3 = GatewayServiceNode(initialVolumeOptions = volumes, initialNetworkOptions = networks, parent = canvas).apply {
            bindYamlPreview()
            connectionObserver.observeConnection(this)
        }
        val testNode2 = GenericDockerServiceNode(
            initialVolumeOptions = volumes,
            initialNetworkOptions = networks,
        ).apply {
            addServiceModelChangeListener {
                updatePreview()
            }
        }

        canvas.add(testNode1)
        canvas.add(testNode2)
        canvas.add(testNode3)
    }

    companion object {
        private val YAML = Yaml(
            configuration = YamlConfiguration(
                encodingIndentationSize = 2,
                singleLineStringStyle = SingleLineStringStyle.Plain,
                multiLineStringStyle = MultiLineStringStyle.Folded,
                sequenceStyle = SequenceStyle.Block,
                encodeDefaults = false,
            )
        )
    }

    private inner class ConnectionObserver {
        var inProgressConnection: GatewayNodeConnector? = null
            set(value) {
                field = value
                toolTipText = if (value != null) {
                    "Press ESC to cancel adding a connection."
                } else {
                    ""
                }
            }

        fun handleConnectionInit(node: GatewayServiceNode) {
            if (inProgressConnection == null) {
                val index = node.connections.keys.maxOrNull()?.plus(1) ?: 1
                val connection = GatewayNodeConnector(node, index)
                node.connections[index] = connection

                canvas.add(connection)

                inProgressConnection = connection
            } else {
                inProgressConnection!!.to = node
                inProgressConnection = null
            }
        }

        fun observeConnection(node: GatewayServiceNode) {
            node.addConnectionInitListener {
                handleConnectionInit(node)
            }
        }
    }
}

object DockerTool : Tool {
    override val serialKey: String = "docker"
    override val title: String = "Docker Draft"
    override val description: String = "Open or create docker-compose.yaml files."
    override val icon: FlatSVGIcon = FlatSVGIcon("icons/bx-docker.svg")

    override fun open(path: Path): ToolPanel {
        return DockerDraftPanel()
    }

    override val filter: FileFilter = FileFilter("YAML files", "yaml", "yml")
}
