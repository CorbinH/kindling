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
import io.github.inductiveautomation.kindling.docker.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.model.DockerVolume
import io.github.inductiveautomation.kindling.docker.ui.AbstractDockerServiceNode
import io.github.inductiveautomation.kindling.docker.ui.Canvas
import io.github.inductiveautomation.kindling.docker.ui.CanvasNodeList
import io.github.inductiveautomation.kindling.docker.ui.GatewayNodeConnector
import io.github.inductiveautomation.kindling.docker.ui.GatewayServiceNode
import io.github.inductiveautomation.kindling.docker.ui.GenericDockerServiceNode
import io.github.inductiveautomation.kindling.utils.FileFilter
import io.github.inductiveautomation.kindling.utils.FlatScrollPane
import io.github.inductiveautomation.kindling.utils.HorizontalSplitPane
import io.github.inductiveautomation.kindling.utils.traverseChildren
import kotlinx.serialization.encodeToString
import net.miginfocom.swing.MigLayout
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_YAML
import java.awt.EventQueue
import java.awt.Font
import java.awt.event.ContainerEvent
import java.awt.event.ContainerListener
import java.nio.file.Path
import javax.swing.JLabel
import javax.swing.JPanel

@Suppress("unused")
class DockerDraftPanel : ToolPanel("ins 0, fill, hidemode 3") {
    override val icon = DockerTool.icon

    private val canvas = Canvas("Docker Drafting")

    private val optionsLabel = JLabel("Components").apply {
        font = font.deriveFont(Font.BOLD, 14F)
    }
    private val optionList = CanvasNodeList()

    private val previewLabel = JLabel("YAML Preview").apply {
        font = font.deriveFont(Font.BOLD, 14F)
    }
    private val yamlPreview = RSyntaxTextArea().apply {
        theme = Kindling.Preferences.UI.Theme.currentValue
        syntaxEditingStyle = SYNTAX_STYLE_YAML
    }

    private val sidebar = JPanel(MigLayout("flowy, nogrid, fill")).apply {
        add(optionsLabel, "gapbottom 3")
        add(optionList, "gapbottom 10, wmin 300, growx")
        add(previewLabel, "gapbottom 3")
        add(FlatScrollPane(yamlPreview), "pushy, grow, wmin 300")
    }

    private val services: List<AbstractDockerServiceNode<*>>
        get() = canvas.traverseChildren(false).filterIsInstance<AbstractDockerServiceNode<*>>().toList()

    private val volumes: MutableSet<DockerVolume> = mutableSetOf(
        DockerVolume("db-data"),
        DockerVolume("backup-data")
    )

    private val networks: Set<DockerNetwork> = mutableSetOf(
        DockerNetwork("network1"),
        DockerNetwork("network2"),
    )

    init {
        val testNode1 = GatewayServiceNode(volumeOptions = volumes, networks = networks).apply {
            model.addServiceModelChangeListener {
                updatePreview()
            }
        }
        val testNode3 = GatewayServiceNode(volumeOptions = volumes, networks = networks).apply {
            model.addServiceModelChangeListener {
                updatePreview()
            }
        }
        val testNode2 = GenericDockerServiceNode(
            initialVolumeOptions = volumes,
            initialNetworkOptions = networks,
        ).apply {
            model.addServiceModelChangeListener {
                updatePreview()
            }
        }

        val testConnector = GatewayNodeConnector(testNode1, testNode3)

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

        canvas.add(testNode1)
        canvas.add(testNode2)
        canvas.add(testNode3)
        canvas.add(testConnector)

        testConnector.setBounds(100, 100, 100, 100)

        yamlPreview.text = YAML.encodeToString(
            mapOf(
                "services" to mapOf(
                    "gateway" to testNode1.model,
                    "db" to testNode2.model,
                ),
            )
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