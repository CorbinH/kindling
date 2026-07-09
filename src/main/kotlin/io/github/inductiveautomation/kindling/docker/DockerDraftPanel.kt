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
import com.formdev.flatlaf.extras.components.FlatTabbedPane
import com.formdev.flatlaf.extras.components.FlatTabbedPane.TabType
import io.github.inductiveautomation.kindling.core.CustomIconView
import io.github.inductiveautomation.kindling.core.Detail
import io.github.inductiveautomation.kindling.core.DetailsPane
import io.github.inductiveautomation.kindling.core.EditorTool
import io.github.inductiveautomation.kindling.core.Kindling
import io.github.inductiveautomation.kindling.core.StackShutdownBehavior
import io.github.inductiveautomation.kindling.core.Theme.Companion.theme
import io.github.inductiveautomation.kindling.core.ToolPanel
import io.github.inductiveautomation.kindling.docker.Canvas.Companion.NODE_LAYER
import io.github.inductiveautomation.kindling.docker.DockerServiceToolTransferHandler.Companion.DOCKER_SERVICE_DATA_FLAVOR
import io.github.inductiveautomation.kindling.docker.engine.ComposeStatus
import io.github.inductiveautomation.kindling.docker.engine.ContainerStats
import io.github.inductiveautomation.kindling.docker.engine.ProcessComposeEngine
import io.github.inductiveautomation.kindling.docker.error.ErrorRegistry
import io.github.inductiveautomation.kindling.docker.error.ErrorsTab
import io.github.inductiveautomation.kindling.docker.networks.NetworksTab
import io.github.inductiveautomation.kindling.docker.networks.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.services.AbstractDockerServiceNode
import io.github.inductiveautomation.kindling.docker.services.DockerServiceTool
import io.github.inductiveautomation.kindling.docker.services.ignition.IgnitionNodeConnector.Companion.midPoint
import io.github.inductiveautomation.kindling.docker.services.ignition.IgnitionServiceNode
import io.github.inductiveautomation.kindling.docker.services.ignition.IgnitionServiceTool
import io.github.inductiveautomation.kindling.docker.services.ignition.model.IgnitionCommandLineArgument
import io.github.inductiveautomation.kindling.docker.services.model.DefaultDockerServiceModel
import io.github.inductiveautomation.kindling.docker.volumes.VolumesTab
import io.github.inductiveautomation.kindling.docker.volumes.model.BindMount
import io.github.inductiveautomation.kindling.docker.volumes.model.DockerVolume
import io.github.inductiveautomation.kindling.utils.Action
import io.github.inductiveautomation.kindling.utils.FileFilter
import io.github.inductiveautomation.kindling.utils.FlatScrollPane
import io.github.inductiveautomation.kindling.utils.HorizontalSplitPane
import io.github.inductiveautomation.kindling.utils.PointHelpers.component1
import io.github.inductiveautomation.kindling.utils.PointHelpers.component2
import io.github.inductiveautomation.kindling.utils.TabStrip
import io.github.inductiveautomation.kindling.utils.TrivialListDataListener
import io.github.inductiveautomation.kindling.utils.VerticalSplitPane
import io.github.inductiveautomation.kindling.utils.chooseFiles
import io.github.inductiveautomation.kindling.utils.jFrame
import io.github.inductiveautomation.kindling.utils.traverseChildren
import kotlinx.serialization.encodeToString
import net.miginfocom.swing.MigLayout
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants.SYNTAX_STYLE_YAML
import java.awt.Point
import java.awt.datatransfer.DataFlavor
import java.awt.event.ContainerEvent
import java.awt.event.ContainerListener
import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.TransferHandler
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.random.Random

class DockerDraftPanel(existingFile: Path?) : ToolPanel("ins 0, fill, hidemode 3") {
    override val icon = DockerTool.icon

    val canvas = Canvas("Docker Drafting").apply {
        transferHandler = object : TransferHandler() {
            override fun canImport(support: TransferSupport?): Boolean {
                if (support == null) return false
                return support.isDataFlavorSupported(DOCKER_SERVICE_DATA_FLAVOR) ||
                    support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
            }

            @Suppress("UNCHECKED_CAST")
            override fun importData(support: TransferSupport?): Boolean {
                support ?: return false
                if (!canImport(support)) return false

                val canvas = support.component as? Canvas ?: return false
                val dropPoint = support.dropLocation.dropPoint

                if (support.isDataFlavorSupported(DOCKER_SERVICE_DATA_FLAVOR)) {
                    val tool = support.transferable.getTransferData(DOCKER_SERVICE_DATA_FLAVOR) as? DockerServiceTool ?: return false
                    val node = tool.createNode(tool.createModel()).apply { bindYamlPreview() }
                    val loc = Point(dropPoint.x - node.preferredSize.width / 2, dropPoint.y - node.preferredSize.height / 2)
                    canvas.add(node, loc)
                    canvas.setLayer(node, NODE_LAYER)
                    return true
                }

                if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    val files = support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
                    val gwbkFiles = files.filterIsInstance<File>().filter { it.extension.equals("gwbk", ignoreCase = true) }
                    if (gwbkFiles.isEmpty()) return false

                    var offset = 0
                    for (file in gwbkFiles) {
                        val model = try {
                            IgnitionServiceTool.createModelFromGwbk(file.toPath())
                        } catch (e: Exception) {
                            JOptionPane.showMessageDialog(
                                null,
                                "Couldn't read GWBK file ${file.name}:\n${e.message}",
                                "Import Error",
                                JOptionPane.ERROR_MESSAGE,
                            )
                            continue
                        }
                        val node = IgnitionServiceTool.createNode(model).apply { bindYamlPreview() }
                        val loc = Point(dropPoint.x - node.preferredSize.width / 2 + offset, dropPoint.y - node.preferredSize.height / 2 + offset)
                        canvas.add(node, loc)
                        canvas.setLayer(node, NODE_LAYER)
                        offset += 20
                    }
                    return true
                }

                return false
            }
        }
    }

    val services: List<AbstractDockerServiceNode<*>>
        get() = canvas.traverseChildren(false).filterIsInstance<AbstractDockerServiceNode<*>>().toList()

    val serviceData = mutableMapOf<String, Any>()

    val nodeIdManager = NodeIdManager()
    val defaultPortManager = DefaultPortManager()

    private var currentFile: Path? = existingFile

    val errorRegistry = ErrorRegistry(
        nodesProvider = { services },
        baseDirProvider = { currentFile?.toAbsolutePath()?.parent },
    )

    /* Sidebar */
    var volumes: List<DockerVolume> = emptyList()
        set(value) {
            field = value
            services.forEach {
                it.volumeOptions = value
            }
        }
    private val volumesTab = VolumesTab(volumes).apply {
        volumesList.model.addListDataListener(
            TrivialListDataListener {
                volumes = List<DockerVolume>(volumesList.model.size) {
                    volumesList.model.getElementAt(it)
                }
                updatePreview()
                errorRegistry.refresh()
            },
        )
    }

    // services forEach update networkOptions
    val networks: Map<String, DockerNetwork> = mutableMapOf()
    private val networksTab = NetworksTab(networks as MutableMap) {
        services.forEach {
            it.networkOptions = networks.keys.toList()
        }
        updatePreview()
        errorRegistry.refresh()
    }

    private val servicesList = CanvasNodeList(DockerServiceTool.tools)

    private val importButton = JButton("Import")
    private val exportButton = JButton("Export")

    private val errorsTab = ErrorsTab(errorRegistry)

    private val sidebarTabStrip = TabStrip().apply {
        isTabsClosable = false
        tabType = TabType.card
        tabHeight = 16
        tabPlacement = SwingConstants.RIGHT
        tabRotation = FlatTabbedPane.TabRotation.auto

        addTab("Nodes", servicesList)
        addTab("Volumes", volumesTab)
        addTab("Networks", networksTab)
        addTab("Errors", errorsTab)
    }

    private val kindlingId = "kindling-${existingFile?.absolutePathString()?.hash() ?: UUID.randomUUID()}"

    private val controlBar = DockerControlBar(
        engine = ProcessComposeEngine(
            projectName = kindlingId,
            composeProvider = { composeFile },
            serializer = { YAML.encodeToString(it) },
            // Relative gwbk bind paths resolve against the compose file's dir; keep it next to them.
            workingDirProvider = { currentFile?.toAbsolutePath()?.parent },
        ),
        yamlPath = existingFile ?: Path.of("docker-compose.yaml"),
        localHashProvider = { currentHash },
        onStatusChange = { onStackStatusChanged(it) },
        onStats = { routeStats(it) },
    )

    // Whether the compose stack is currently running, mirrored from the control bar's polling.
    private var stackRunning = false

    private fun onStackStatusChanged(status: ComposeStatus) {
        stackRunning = status is ComposeStatus.Running || status is ComposeStatus.Partial
        services.filterIsInstance<IgnitionServiceNode>().forEach { it.onStackRunningChanged(stackRunning) }
    }

    private fun routeStats(stats: Map<String, ContainerStats>) {
        services.filterIsInstance<IgnitionServiceNode>().forEach { it.updateStats(stats[it.model.containerName]) }
    }

    private val sidebar = JPanel(MigLayout("fill, ins 0")).apply {
        add(importButton, "growx, pushx, sg")
        add(exportButton, "growx, pushx, wrap")
        add(sidebarTabStrip, "push, grow, span")
    }

    /* YAML Preview */
    private val popoutPreviewTextArea = RSyntaxTextArea().apply {
        theme = Kindling.Preferences.UI.Theme.currentValue
        syntaxEditingStyle = SYNTAX_STYLE_YAML

        Kindling.Preferences.UI.Theme.addChangeListener {
            theme = it
        }
    }

    private val popoutPreview = jFrame("YAML Preview", 800, 600, false) {
        add(FlatScrollPane(popoutPreviewTextArea))
        defaultCloseOperation = JFrame.HIDE_ON_CLOSE
    }

    private val detailPane = DetailsPane().apply {
        actions.add(
            Action(description = "Popout", icon = FlatSVGIcon("icons/bx-detail.svg")) {
                popoutPreview.isVisible = true
            },
        )
    }

    private val currentHash: String
        get() {
            val baseFile = DockerComposeFile(
                null,
                services.map { it.model.defaultModel }.sortedBy { it.containerName },
                volumes,
                networks,
            )
            return if (baseFile.isEmpty()) "" else YAML.encodeToString(baseFile).hash()
        }

    private val composeFile: DockerComposeFile
        get() {
            val hash = currentHash
            return DockerComposeFile(
                null,
                services.map { node ->
                    // We need a shallow copy to add the hash label for Docker
                    val model = node.model.defaultModel
                    DefaultDockerServiceModel(
                        image = model.image,
                        hostName = model.hostName,
                        containerName = model.containerName,
                        ports = model.ports.toMutableList(),
                        environment = model.environment.toMutableMap(),
                        commands = model.commands.toMutableList(),
                        volumes = model.volumes.toMutableList(),
                        networks = model.networks.toMutableMap(),
                        // Hash injected here (not on the model) to avoid a circular dependency: currentHash is computed from the label-free model
                        labels = model.labels.toMutableMap().apply {
                            put("io.github.kindling.yaml-hash", hash)
                        },
                        dependsOn = model.dependsOn.toMutableMap(),
                        envFile = model.envFile.toMutableList(),
                        attach = model.attach,
                        build = model.build,
                        deploy = model.deploy,
                        entrypoint = model.entrypoint.toMutableList(),
                        restart = model.restart,
                        pullPolicy = model.pullPolicy,
                        readOnly = model.readOnly,
                        user = model.user,
                        capAdd = model.capAdd.toMutableList(),
                        capDrop = model.capDrop.toMutableList(),
                        securityOpt = model.securityOpt.toMutableList(),
                        cGroup = model.cGroup,
                        pid = model.pid,
                    ).apply {
                        canvasLocation = model.canvasLocation
                    }
                }.sortedBy { it.containerName },
                volumes,
                networks,
            )
        }

    init {
        name = existingFile?.name ?: "New Editor"
        toolTipText = existingFile?.absolutePathString() ?: ""

        errorRegistry.addErrorsChangedListener { errors ->
            val index = sidebarTabStrip.indexOfComponent(errorsTab)
            if (index < 0) return@addErrorsChangedListener
            sidebarTabStrip.setTitleAt(index, if (errors.isEmpty()) "Errors" else "Errors (${errors.size})")
        }

        val innerSplitPane = HorizontalSplitPane(
            left = canvas,
            right = sidebar,
            resizeWeight = 0.5,
            expandableSide = FlatSplitPane.ExpandableSide.left,
        )

        add(controlBar, "growx, wrap")
        add(
            VerticalSplitPane(
                top = innerSplitPane,
                bottom = detailPane,
                resizeWeight = 0.1,
            ),
            "push, grow",
        )

        canvas.addContainerListener(
            object : ContainerListener {
                override fun componentAdded(e: ContainerEvent?) {
                    updatePreview()
                    errorRegistry.refresh()
                    // A node added while the stack is running should reflect that immediately.
                    (e?.child as? IgnitionServiceNode)?.onStackRunningChanged(stackRunning)
                }

                override fun componentRemoved(e: ContainerEvent?) {
                    updatePreview()
                    errorRegistry.refresh()
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
            val restoreFlag = IgnitionCommandLineArgument.GWBK_RESTORE_PATH.flag
            val hasGwbk = services.any { node ->
                node.model.defaultModel.commands.any { it.startsWith("$restoreFlag ") }
            }
            if (!hasGwbk) {
                yamlFileChooser.approveButtonText = "Export"
                export()
                return@addActionListener
            }
            val mode = showExportDialog(this) ?: return@addActionListener
            when (mode) {
                is ExportMode.Standalone -> {
                    yamlFileChooser.approveButtonText = "Export"
                    export()
                }
                is ExportMode.Bundle -> exportBundle(mode.placement)
            }
        }

        SwingUtilities.invokeLater {
            innerSplitPane.apply {
                dividerLocation = this@apply.size.width -
                    this@apply.insets.right -
                    rightComponent.minimumSize.width - dividerSize
            }

            SwingUtilities.invokeLater {
                if (existingFile != null) {
                    import(existingFile)
                }

                updatePreview()

                DockerServiceTool.tools.forEach {
                    with(it) {
                        init()
                    }
                }
            }
        }
    }

    private fun AbstractDockerServiceNode<*>.bindYamlPreview() {
        model.addServiceModelChangeListener {
            updatePreview()
            errorRegistry.refresh()
        }
    }

    private fun export() {
        yamlFileChooser.selectedFile = yamlFileChooser.currentDirectory.resolve("docker-compose.yaml")
        if (yamlFileChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return
        val outputFile = yamlFileChooser.selectedFile.toPath()

        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile)
        }

        outputFile.outputStream().use {
            YAML.encodeToStream(composeFile, it)
        }
    }

    // Replace any absolute path GWBKs with relate path for the bundle.
    private fun exportBundle(placement: GwbkPlacement) {
        val folderChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isAcceptAllFileFilterUsed = false
            approveButtonText = "Export"
            dialogTitle = "Choose Bundle Folder"
        }
        if (folderChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return
        val folder = folderChooser.selectedFile.toPath()
        Files.createDirectories(folder)

        val restoreFlag = IgnitionCommandLineArgument.GWBK_RESTORE_PATH.flag
        val restoreMounts = services.mapNotNull { node ->
            val model = node.model.defaultModel
            val restoreArg = model.commands.firstOrNull { it.startsWith("$restoreFlag ") } ?: return@mapNotNull null
            val restoreContainerPath = restoreArg.substringAfter("$restoreFlag ").trim()
            model.volumes.firstOrNull { it.containerPath == restoreContainerPath }
        }

        val originalPaths = mutableMapOf<BindMount, String>()
        try {
            for (mount in restoreMounts) {
                val source = Path.of(mount.bindPath)
                if (!Files.exists(source)) continue
                val target = folder / source.fileName
                when (placement) {
                    GwbkPlacement.COPY -> Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
                    GwbkPlacement.MOVE -> Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
                }
                originalPaths[mount] = mount.bindPath
                mount.bindPath = "./${source.fileName}"
            }

            val yamlPath = folder.resolve("docker-compose.yaml")
            yamlPath.outputStream().use {
                YAML.encodeToStream(composeFile, it)
            }
            // The bundle is now the backing file, so relative gwbk paths resolve against its folder.
            currentFile = yamlPath
        } finally {
            if (placement == GwbkPlacement.COPY) {
                for ((mount, original) in originalPaths) {
                    mount.bindPath = original
                }
            }
            updatePreview()
        }
    }

    private fun import(importFile: Path) {
        fun createNodes(services: List<DefaultDockerServiceModel>): List<AbstractDockerServiceNode<*>> {
            return services.mapNotNull { model ->
                val tool = DockerServiceTool.tools.find {
                    it.isValidCandidate(model)
                } ?: return@mapNotNull null

                tool.createNode(tool.modelFromDefault(model))
            }.onEach {
                it.bindYamlPreview()
            }
        }

        fun layoutComponents(nodes: List<AbstractDockerServiceNode<*>>) {
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
            e.printStackTrace()
            return
        }

        (networks as MutableMap).putAll(composeFile.networks)
        networksTab.table.model.fireTableDataChanged()

        (volumesTab.volumesList.model as DefaultListModel<DockerVolume>).addAll(composeFile.volumes)

        currentFile = importFile
        val nodes = createNodes(composeFile.services)
        layoutComponents(nodes)
        errorRegistry.refresh()
    }

    private fun clear() {
        canvas.removeAll()
    }

    /** Applies the user's configured shutdown behavior to this panel's stack (called on app close). */
    fun applyStackShutdownBehavior(behavior: StackShutdownBehavior) = controlBar.applyShutdownBehavior(behavior)

    private fun updatePreview() {
        val text = runCatching {
            val c = composeFile
            if (c.isEmpty()) "" else YAML.encodeToString(composeFile)
        }.getOrElse { error ->
            error.stackTraceToString()
        }

        detailPane.events = listOf(
            Detail(
                "YAML Preview",
                body = text.split("\n"),
            ),
        )
        popoutPreviewTextArea.text = text
        // Re-evaluate the In Sync / Stale indicator against the edited model (no Docker call).
        controlBar.onModelChanged()
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

        fun String.hash(): String {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(toByteArray())
            return BigInteger(1, digest).toString(16).padStart(32, '0').take(8)
        }
    }

    inner class NodeIdManager {
        private val seenIDs = services.map {
            it.name.takeLastWhile { c -> c.isDigit() }.toInt()
        }.toMutableList()

        fun generateID(): Int {
            var newID = Random.nextInt(100000) // Not going to bother with collisions since they're extremely unlikely.
            while (newID in seenIDs) {
                newID = Random.nextInt(100000)
            }
            seenIDs.add(newID)
            return newID
        }
    }

    inner class DefaultPortManager {
        private val usedPorts: List<UShort>
            get() = services.flatMap { node ->
                node.model.ports.mapNotNull { portMapping ->
                    portMapping.published.toUShortOrNull()
                }
            }

        fun requestPorts(numPorts: Int): List<UShort> = buildList {
            val p = usedPorts.toMutableList()
            var newPort: UShort = 9088u
            repeat(numPorts) {
                while (newPort in p) {
                    newPort++
                }
                p.add(newPort)
                add(newPort)
            }
        }
    }
}

object DockerTool : EditorTool {
    override val serialKey: String = "docker"
    override val title: String = "Docker Draft"
    override val description: String = "Open or create docker-compose.yaml files."
    override val icon: FlatSVGIcon = FlatSVGIcon("icons/bx-docker.svg")
    override val extensions: Array<String> = arrayOf("yaml", "yml")

    override fun open(path: Path): ToolPanel = DockerDraftPanel(path)

    override fun open(): ToolPanel = DockerDraftPanel(null)

    override val filter: FileFilter = FileFilter("YAML files", "yaml", "yml")
}
