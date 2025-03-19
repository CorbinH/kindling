package io.github.inductiveautomation.kindling.docker

import com.charleskorn.kaml.MultiLineStringStyle
import com.charleskorn.kaml.SequenceStyle
import com.charleskorn.kaml.SingleLineStringStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.decodeFromStream
import com.charleskorn.kaml.encodeToStream
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.extras.components.FlatSplitPane
import io.github.inductiveautomation.kindling.core.CustomIconView
import io.github.inductiveautomation.kindling.core.EditorTool
import io.github.inductiveautomation.kindling.core.Kindling
import io.github.inductiveautomation.kindling.core.Theme.Companion.theme
import io.github.inductiveautomation.kindling.core.ToolPanel
import io.github.inductiveautomation.kindling.docker.model.DefaultDockerServiceModel
import io.github.inductiveautomation.kindling.docker.model.DefaultDockerServiceModel.Companion.DEFAULT_GENERIC_IMAGE
import io.github.inductiveautomation.kindling.docker.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.model.DockerServiceModel
import io.github.inductiveautomation.kindling.docker.model.DockerVolume
import io.github.inductiveautomation.kindling.docker.model.GatewayEnvironmentVariableDefinition.Companion.getConnectionVariableIndex
import io.github.inductiveautomation.kindling.docker.model.GatewayServiceModel
import io.github.inductiveautomation.kindling.docker.model.GatewayServiceModel.Companion.DEFAULT_IMAGE
import io.github.inductiveautomation.kindling.docker.model.PortMapping
import io.github.inductiveautomation.kindling.docker.ui.AbstractDockerServiceNode
import io.github.inductiveautomation.kindling.docker.ui.Canvas
import io.github.inductiveautomation.kindling.docker.ui.CanvasNodeList
import io.github.inductiveautomation.kindling.docker.ui.ConnectionProgressChangeListener
import io.github.inductiveautomation.kindling.docker.ui.GatewayNodeConnector
import io.github.inductiveautomation.kindling.docker.ui.GatewayNodeConnector.Companion.midPoint
import io.github.inductiveautomation.kindling.docker.ui.GatewayServiceNode
import io.github.inductiveautomation.kindling.docker.ui.GenericDockerServiceNode
import io.github.inductiveautomation.kindling.docker.ui.NetworksList
import io.github.inductiveautomation.kindling.docker.ui.NodeInitializer
import io.github.inductiveautomation.kindling.docker.ui.VolumesList
import io.github.inductiveautomation.kindling.utils.FileFilter
import io.github.inductiveautomation.kindling.utils.FlatScrollPane
import io.github.inductiveautomation.kindling.utils.HorizontalSplitPane
import io.github.inductiveautomation.kindling.utils.PointHelpers.component1
import io.github.inductiveautomation.kindling.utils.PointHelpers.component2
import io.github.inductiveautomation.kindling.utils.TrivialListDataListener
import io.github.inductiveautomation.kindling.utils.add
import io.github.inductiveautomation.kindling.utils.chooseFiles
import io.github.inductiveautomation.kindling.utils.getAll
import io.github.inductiveautomation.kindling.utils.traverseChildren
import java.awt.EventQueue
import java.awt.Font
import java.awt.KeyboardFocusManager
import java.awt.Point
import java.awt.event.ContainerEvent
import java.awt.event.ContainerListener
import java.awt.event.KeyEvent
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.random.Random
import kotlinx.serialization.encodeToString
import net.miginfocom.swing.MigLayout
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_YAML

@Suppress("unused")
class DockerDraftPanel(existingFile: Path?) : ToolPanel("ins 0, fill, hidemode 3") {
    override val icon = DockerTool.icon

    private val canvas = Canvas("Docker Drafting")

    private val services: List<AbstractDockerServiceNode<*>>
        get() = canvas.traverseChildren(false).filterIsInstance<AbstractDockerServiceNode<*>>().toList()

    private val nodeIdManager = object {
        val seenIDs = mutableListOf<Int>()
        fun generateID(): Int {
            var newID = Random.nextInt(10000)
            while (newID in seenIDs) {
                newID = Random.nextInt(10000)
            }
            seenIDs.add(newID)
            return newID
        }
    }

    private val portMapper = object {
        val usedPorts = mutableListOf<UShort>()
        fun generatePort(): UShort {
            var newPort: UShort = 9088u
            while (newPort in usedPorts) {
                newPort++
            }
            usedPorts.add(newPort)
            return newPort
        }

        fun listenForDeletion(node: AbstractDockerServiceNode<*>) {
            node.addNodeDeleteListener {
                usedPorts.removeAll(node.model.ports.map { it.hostPort })
            }
        }
    }

    private var volumes: List<DockerVolume> = emptyList()
        set(value) {
            field = value
            services.forEach {
                it.volumeOptions = value
            }
        }

    private var networks: List<DockerNetwork> = emptyList()
        set(value) {
            field = value
            services.forEach {
                it.networkOptions = value
            }
        }

    private val connectionObserver = ConnectionObserver()

    init {
        if (existingFile != null) {
            import(existingFile)
        }
    }

    private val optionsLabel = JLabel("Components").apply {
        font = font.deriveFont(Font.BOLD, 14F)
    }
    private val optionList = CanvasNodeList(
        listOf(
            NodeInitializer(
                DockerTool.ignitionIcon,
                "Ignition Gateway Node",
            ) {
                GatewayServiceNode(
                    GatewayServiceModel(
                        image = DEFAULT_IMAGE,
                        containerName = "Ignition-${nodeIdManager.generateID()}",
                        ports = mutableListOf(PortMapping(portMapper.generatePort(), 8088u)),
                    ),
                    initialNetworkOptions = networks,
                    initialVolumeOptions = volumes,
                ).apply {
                    bindYamlPreview()
                    connectionObserver.observeConnection(this)
                    portMapper.listenForDeletion(this)
                }
            },
            NodeInitializer(
                DockerTool.icon,
                "Generic Docker Node",
            ) {
                GenericDockerServiceNode(
                    DefaultDockerServiceModel(
                        image = DEFAULT_GENERIC_IMAGE,
                        containerName = "Container-${nodeIdManager.generateID()}",
                    ),
                    initialVolumeOptions = volumes,
                    initialNetworkOptions = networks,
                ).apply {
                    bindYamlPreview()
                }
            },
        ),
    )

    private val previewLabel = JLabel("YAML Preview").apply {
        font = font.deriveFont(Font.BOLD, 14F)
    }
    private val yamlPreview = RSyntaxTextArea().apply {
        theme = Kindling.Preferences.UI.Theme.currentValue
        syntaxEditingStyle = SYNTAX_STYLE_YAML

        Kindling.Preferences.UI.Theme.addChangeListener {
            theme = it
        }
    }

    private val volumesLabel = JLabel("Volumes").apply {
        font = font.deriveFont(Font.BOLD, 14F)
    }
    private val volumesList = VolumesList(volumes).apply {
        volumesList.model.addListDataListener(
            TrivialListDataListener {
                val options = Array<DockerVolume>(volumesList.model.size) {
                    volumesList.model.getElementAt(it)
                }
                volumes = options.toList()
                updatePreview()
            },
        )
    }

    private val networksLabel = JLabel("Networks").apply {
        font = font.deriveFont(Font.BOLD, 14F)
    }
    private val networksList = NetworksList(networks).apply {
        networksList.model.addListDataListener(
            TrivialListDataListener {
                val options = Array<DockerNetwork>(networksList.model.size) {
                    networksList.model.getElementAt(it)
                }
                networks = options.toList()
                updatePreview()
            },
        )
    }

    private val importButton = JButton("Import Compose File")
    private val exportButton = JButton("Export Compose File")

    private val sidebar = JPanel(MigLayout("flowy, nogrid, fill")).apply {
        add(
            JPanel(MigLayout("fill, ins 0")).apply {
                add(importButton, "grow, sg")
                add(exportButton, "grow, sg")
            },
            "growx, gapbottom 3",
        )
        add(optionsLabel, "gapbottom 3")
        add(optionList, "gapbottom 10, wmin 300, growx")
        add(volumesLabel, "gapbottom 3")
        add(volumesList, "grow, gapbottom 3, sg, hmin 100")
        add(networksLabel, "gapbottom 3")
        add(networksList, "grow, gapbottom 10, sg, hmin 100")
        add(previewLabel, "gapbottom 3")
        add(FlatScrollPane(yamlPreview), "pushy, grow, wmin 300")
    }

    private val composeFile: DockerComposeFile
        get() {
            return DockerComposeFile(
                services.map { it.model }.sortedBy { it.containerName },
                volumes,
                networks,
            )
        }

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
            },
            "push, grow",
        )

        canvas.addContainerListener(
            object : ContainerListener {
                override fun componentAdded(e: ContainerEvent?) {
                    updatePreview()
                }

                override fun componentRemoved(e: ContainerEvent?) {
                    updatePreview()
                }
            },
        )

        importButton.addActionListener {
            yamlFileChooser.approveButtonText = "Import"
            val file = composeFile
            if (file.volumes.isNotEmpty() || file.networks.isNotEmpty() || file.services.isNotEmpty()) {
                val confirm = JOptionPane.showConfirmDialog(
                    null,
                    "There are existing changes. Importing will erase them. Proceed?",
                    "Changes Detected",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                )

                if (confirm != JOptionPane.YES_OPTION) return@addActionListener
            }

            val importFile = yamlFileChooser.chooseFiles(null)?.firstOrNull()?.toPath() ?: return@addActionListener
            clear()
            import(importFile)
        }

        exportButton.addActionListener {
            yamlFileChooser.approveButtonText = "Export"
            export()
        }

        SwingUtilities.invokeLater {
            updatePreview()
        }
    }

    private fun AbstractDockerServiceNode<*>.bindYamlPreview() {
        addServiceModelChangeListener {
            updatePreview()
        }
    }

    private fun export() {
        yamlFileChooser.selectedFile = yamlFileChooser.currentDirectory.resolve("docker-compose.yaml")
        val outputFile = yamlFileChooser.chooseFiles(null)?.firstOrNull()?.toPath() ?: return

        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile)
        }

        outputFile.outputStream().use {
            YAML.encodeToStream(composeFile, it)
        }
    }

    private fun import(importFile: Path) {
        fun createNodes(services: List<DockerServiceModel>): List<AbstractDockerServiceNode<*>> {
            return services.map { model ->
                when (model) {
                    is GatewayServiceModel -> {
                        GatewayServiceNode(model, volumes, networks).apply {
                            bindYamlPreview()
                            connectionObserver.observeConnection(this)
                        }
                    }

                    is DefaultDockerServiceModel -> {
                        GenericDockerServiceNode(model, volumes, networks).apply {
                            bindYamlPreview()
                        }
                    }
                }
            }
        }

        fun resolveConnections(nodes: List<AbstractDockerServiceNode<*>>): List<GatewayNodeConnector> {
            val connections = mutableListOf<GatewayNodeConnector>()

            for (node in nodes) {
                if (node is GatewayServiceNode) {
                    val outboundHosts = node.model.environment.filter { (k, _) ->
                        k.startsWith("GATEWAY_NETWORK_") && k.endsWith("_HOST")
                    }.map {
                        it.key.getConnectionVariableIndex()!! to it.value
                    }

                    for (host in outboundHosts) {
                        val outboundNode = nodes.find { it.model.hostName == host.second } as GatewayServiceNode
                        val connection = GatewayNodeConnector(node, host.first, canvas).apply {
                            to = outboundNode
                        }
                        node.connections[host.first] = connection
                        connections.add(connection)
                    }
                }
            }

            return connections
        }

        fun layoutComponents(nodes: List<AbstractDockerServiceNode<*>>, connections: List<GatewayNodeConnector>) {
            var collateOffset = 0
            for (node in nodes) {
                val p = node.model.canvasLocation ?: run {
                    val (xC, yC) = canvas.midPoint()
                    val p = Point(xC + collateOffset, yC + collateOffset)
                    collateOffset += 10
                    p
                }
                canvas.add(node, p)
                canvas.setComponentZOrder(node, 0)
            }

            for (c in connections) {
                canvas.add(c)
            }
        }

        fun parseExistingIDs(nodes: List<AbstractDockerServiceNode<*>>) {
            for (node in nodes) {
                val possibleID = node.model.containerName.split("-").getOrNull(1)?.toIntOrNull()
                if (possibleID != null) {
                    nodeIdManager.seenIDs.add(possibleID)
                }
            }
        }

        fun parseExistingPorts(nodes: List<AbstractDockerServiceNode<*>>) {
            val usedPorts = nodes.flatMap { node ->
                node.model.ports.map { mapping ->
                    mapping.hostPort
                }
            }

            portMapper.usedPorts.addAll(usedPorts)
        }

        val composeFile = try {
            importFile.inputStream().use<_, DockerComposeFile>(YAML::decodeFromStream)
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(
                null,
                "Couldn't import docker file:\n${e.message}",
                "Import Error",
                JOptionPane.ERROR_MESSAGE,
            )
            return
        }

        networks = composeFile.networks
        volumes = composeFile.volumes

        val nodes = createNodes(composeFile.services)
        val connections = resolveConnections(nodes)
        layoutComponents(nodes, connections)
        parseExistingIDs(nodes)
        parseExistingPorts(nodes)
    }

    private fun clear() {
        canvas.removeAll()
    }

    private fun updatePreview() {
        yamlPreview.text = runCatching {
            val c = composeFile
            if (c.isEmpty()) "" else YAML.encodeToString(composeFile)
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
                extensionDefinitionPrefix = "x-",
            ),
        )

        private val yamlFileChooser = JFileChooser().apply {
            isMultiSelectionEnabled = false
            isAcceptAllFileFilterUsed = false
            fileView = CustomIconView()
            fileFilter = FileNameExtensionFilter("YAML Files", "yaml", "yml")
            approveButtonText = "Export"
        }
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
                fireConnectionProgressChange()
            }

        init {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher { e ->
                if (e.keyCode == KeyEvent.VK_ESCAPE) {
                    inProgressConnection?.let {
                        canvas.remove(it)
                        it.from.connections.remove(it.index)
                        inProgressConnection = null
                        canvas.repaint()
                    }
                    true
                } else {
                    false
                }
            }
        }

        fun handleConnectionInit(node: GatewayServiceNode) {
            if (inProgressConnection == null) {
                val index = node.connections.keys.maxOrNull()?.plus(1) ?: 1
                val connection = GatewayNodeConnector(node, index, canvas)
                node.connections[index] = connection

                canvas.add(connection)

                inProgressConnection = connection
            } else {
                if (validateConnection(inProgressConnection!!.from, node)) {
                    inProgressConnection!!.to = node
                    inProgressConnection = null
                } else {
                    JOptionPane.showMessageDialog(
                        null,
                        "Please select a different node!",
                        "Invalid Connection",
                        JOptionPane.WARNING_MESSAGE,
                    )
                }
            }
        }

        fun addConnectionProgressChangeListener(l: ConnectionProgressChangeListener) = listenerList.add(l)

        private fun fireConnectionProgressChange() {
            listenerList.getAll<ConnectionProgressChangeListener>().forEach {
                it.onConnectionProgressChangeRequest(inProgressConnection != null)
            }
        }

        private fun validateConnection(from: GatewayServiceNode, to: GatewayServiceNode): Boolean {
            if (from === to) return false
            if (from.model.hostName.isNullOrEmpty() || to.model.hostName.isNullOrEmpty()) return false

            return to.model.networks.any {
                it in from.model.networks
            } || (from.model.networks.isEmpty() || to.model.networks.isEmpty())
        }

        fun observeConnection(node: GatewayServiceNode) {
            node.addConnectionInitListener {
                handleConnectionInit(node)
            }

            addConnectionProgressChangeListener { inProgress: Boolean ->
                node.updateValidConnectionTarget(inProgress)
            }
        }
    }
}

object DockerTool : EditorTool {
    override val serialKey: String = "docker"
    override val title: String = "Docker Draft"
    override val description: String = "Open or create docker-compose.yaml files."
    override val icon: FlatSVGIcon = FlatSVGIcon("icons/bx-docker.svg")

    internal val ignitionIcon: FlatSVGIcon = FlatSVGIcon("icons/Logo-Ignition-Check.svg")

    override fun open(path: Path): ToolPanel {
        return DockerDraftPanel(path)
    }

    override fun open(): ToolPanel {
        return DockerDraftPanel(null)
    }

    override val filter: FileFilter = FileFilter("YAML files", "yaml", "yml")
}
