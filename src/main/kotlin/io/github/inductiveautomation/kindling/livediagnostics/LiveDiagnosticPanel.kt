package io.github.inductiveautomation.kindling.livediagnostics

import com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_TAB_CLOSABLE
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.extras.components.FlatPopupMenu
import com.formdev.flatlaf.extras.components.FlatTabbedPane
import io.github.inductiveautomation.kindling.core.EditorTool
import io.github.inductiveautomation.kindling.core.ToolPanel
import io.github.inductiveautomation.kindling.utils.Action
import io.github.inductiveautomation.kindling.utils.FileFilter
import io.github.inductiveautomation.kindling.utils.HorizontalSplitPane
import io.github.inductiveautomation.kindling.utils.PathNode
import io.github.inductiveautomation.kindling.utils.TabStrip
import io.github.inductiveautomation.kindling.utils.VerticalSplitPane
import io.github.inductiveautomation.kindling.utils.ZipFileModel
import io.github.inductiveautomation.kindling.utils.ZipFileTree
import io.github.inductiveautomation.kindling.utils.attachPopupMenu
import io.github.inductiveautomation.kindling.utils.transferTo
import io.github.inductiveautomation.kindling.zip.ZipViewer.createView
import io.github.inductiveautomation.kindling.zip.views.PathView
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.miginfocom.swing.MigLayout
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.OutputStream
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.spi.FileSystemProvider
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JSeparator
import javax.swing.JSpinner
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.UIManager
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.name

@Serializable
data class BundleStatus(val state: String)

@Serializable
data class BundleComplete(val state: String, val fileSize: Int)

@Serializable
data class LiveDiagnosticConfigFile(val url: String, val key: String)

@Serializable
data class CurrentPerformanceData(val cpu: Double, val heapMemory: Double, val nonHeapMemory: Double? = 0.0)

@Serializable
data class CurrentThreadData(val running: Int, val waiting: Double, val timedWaiting: Double, val blocked: Double)

class LiveDiagnosticPanel(var path: Path?) : ToolPanel("ins 0, fill, hidemode 3") {

    private val apiScope = CoroutineScope(Dispatchers.IO)
    private var resourceJob: Job? = null
    private var threadJob: Job? = null

    override fun getToolTipText(): String = "Live Diagnostic Tool"
    override val icon: Icon = LiveDiagnosticTool.icon

    var provider: FileSystemProvider? = null
    private val tabStrip = TabStrip()
    private val FlatTabbedPane.tabs: Sequence<PathView>
        get() = sequence {
            repeat(tabCount) { i ->
                yield(getComponentAt(i) as PathView)
            }
        }

    val fileTree = ZipFileTree(null)
    val resourceRateSpinner = JSpinner(SpinnerNumberModel(5000L, 500L, Long.MAX_VALUE, 500L))
    val threadRateSpinner = JSpinner(SpinnerNumberModel(5000L, 500L, Long.MAX_VALUE, 500L))

    val liveResourceMetricsButton = JButton("Get Live Values").apply {
        addActionListener {
            if (resourceJob == null && validateUrl(urlField.text)) {
                text = "Stop"
                resourceJob = apiScope.launch {
                    startResourcePolling()
                }
            } else {
                text = "Get Live Values"
                stopResourcePolling()
            }
        }
    }

    val liveThreadsButton = JButton("Capture Threads").apply {
        addActionListener {
            if (threadJob == null && validateUrl(urlField.text)) {
                text = "Stop"
                threadJob = apiScope.launch {
                    startThreadPolling()
                }
            } else {
                text = "Capture Threads"
                stopThreadPolling()
            }
        }
    }

    val resourceStats = JTextArea()
    val threadStats = JTextArea()
    var apiKeyField = JTextField()
    var urlField = JTextField()

    val saveButton = JButton("Save Config").apply {
        addActionListener {
            saveConfigToJson()
        }
    }
    val downloadBundleButton = JButton("Download Bundle").apply {
        addActionListener {
            runBlocking {
                if (!validateUrl(urlField.text)) {
                    cancel()
                }
                executeBundleProcess()
                getLiveData()
            }
        }
    }

    val cpuLabel = JLabel("0.00%").apply { font = font.deriveFont(java.awt.Font.BOLD) }
    val heapLabel = JLabel("0.00 MB").apply { font = font.deriveFont(java.awt.Font.BOLD) }
    val nonHeapLabel = JLabel("0.00 MB").apply { font = font.deriveFont(java.awt.Font.BOLD) }

    val runningThreadsLabel = JLabel("0")
    val waitingThreadsLabel = JLabel("0")
    val timedWaitingThreadsLabel = JLabel("0")
    val blockedThreadsLabel = JLabel("0")

    val leftUpperPanel = JPanel(MigLayout("ins 10, fill, hidemode 3, wrap 1")).apply {
        border = BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"))

        add(
            JPanel(MigLayout("ins 0, fillx")).apply {
                add(JLabel("URL:"), "gapright 10")
                add(urlField, "pushx, growx, wrap")
                add(JLabel("API Key:"), "gapright 10")
                add(apiKeyField, "pushx, growx")
            },
            "growx, pushx",
        )

        add(
            JPanel(MigLayout("ins 0, fillx, wrap 2")).apply {
                add(downloadBundleButton, "growx")
                add(saveButton, "growx")
            },
            "growx",
        )

        add(JSeparator(), "growx, gapy 10 10")

        add(
            JLabel("Resource Metrics").apply {
                font = font.deriveFont(java.awt.Font.BOLD)
                font = font.deriveFont(16f)
            },
            "gapy 10 5",
        )

        add(
            JPanel(MigLayout("ins 10, fillx, wrap 2", "[]20[grow, right]")).apply {
                border = BorderFactory.createTitledBorder("JVM Performance")

                add(JLabel("CPU Usage:"))
                add(cpuLabel)
                add(JLabel("Heap Memory:"))
                add(heapLabel)
                add(JLabel("Non-Heap Memory:"))
                add(nonHeapLabel)

                add(JSeparator(), "span, growx, gapy 5 5")
                add(JLabel("Poll Rate (ms):"))
                add(resourceRateSpinner, "w 80!")
                add(liveResourceMetricsButton, "span, growx, gapy 5 0")
            },
            "growx",
        )

        add(
            JLabel("Thread Metrics").apply {
                font = font.deriveFont(java.awt.Font.BOLD)
                font = font.deriveFont(16f)
            },
            "gapy 10 5",
        )

        add(
            JPanel(MigLayout("ins 10, fillx, wrap 4", "[][grow][][grow]")).apply {
                border = BorderFactory.createTitledBorder("Thread States")

                add(JLabel("Running:"))
                add(runningThreadsLabel)

                add(JLabel("Blocked:"))
                add(blockedThreadsLabel, "wrap")

                add(JLabel("Waiting:"))
                add(waitingThreadsLabel)

                add(JLabel("Timed Wait:"))
                add(timedWaitingThreadsLabel)

                add(JSeparator(), "span, growx, gapy 5 5")
                add(JLabel("Poll Rate (ms):"), "span 2")
                add(threadRateSpinner, "span 2, w 80!, wrap")
                add(liveThreadsButton, "span, growx")
            },
            "growx",
        )
    }
    val leftLowerPanel = JPanel(MigLayout("ins 3, fill, hidemode 3")).apply {
        border = BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"))
        add(fileTree, "grow, push, wrap")
        add(
            JButton("Save Bundle").apply {
                addActionListener {
                }
            },
            "grow",
        )
    }
    val leftPanel = VerticalSplitPane(leftUpperPanel, leftLowerPanel, resizeWeight = 0.5)
    val mainPanel = HorizontalSplitPane(leftPanel, tabStrip, resizeWeight = 0.25)

    init {
        name = "Live Diagnostics"
        add(mainPanel, "push, grow")
        if (path != null) {
            name = path!!.name
            val configValues = readJSONFile(path!!)
            apiKeyField.text = configValues.key
            urlField.text = configValues.url
            runBlocking {
                executeBundleProcess()
            }
        }
        fileTree.addMouseListener(
            object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent?) {
                    if (e?.clickCount == 2) {
                        val pathNode = fileTree.selectionPath?.lastPathComponent as? PathNode ?: return
                        val actualPath = pathNode.userObject
                        maybeAddNewTab(actualPath)
                    }
                }
            },
        )

        fileTree.attachPopupMenu {
            selectionPaths?.let { selectedPaths ->
                FlatPopupMenu().apply {
                    val openIndividually =
                        Action("Open File") {
                            for (treePath in selectedPaths) {
                                val actualPath = (treePath.lastPathComponent as PathNode).userObject
                                maybeAddNewTab(actualPath)
                            }
                        }
                    if (selectedPaths.size > 1) {
                        add(
                            Action("Open in new aggregate view") {
                                val actualPaths =
                                    Array(selectedPaths.size) {
                                        (selectedPaths[it].lastPathComponent as PathNode).userObject
                                    }
                                maybeAddNewTab(*actualPaths)
                            },
                        )
                        openIndividually.name = "Open ${selectedPaths.size} files individually"
                    }
                    add(openIndividually)

                    val selectedNode = selectedPaths.first().lastPathComponent as PathNode

                    if (selectedPaths.size == 1 && selectedNode.isLeaf) {
                        add(
                            Action("Save As") {
                                exportFileChooser.apply {
                                    resetChoosableFileFilters()
                                    selectedFile = File(selectedNode.userObject.name)
                                    if (provider != null && showSaveDialog(this@attachPopupMenu) == JFileChooser.APPROVE_OPTION) {
                                        provider!!.newInputStream(selectedNode.userObject) transferTo selectedFile.outputStream()
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    private val json = Json { prettyPrint = true }
    private var isActive = false

    fun createMetricComponent(labelText: String): Pair<JLabel, JLabel> {
        val nameLabel = JLabel(labelText).apply {
            font = font.deriveFont(java.awt.Font.BOLD)
        }
        val valueLabel = JLabel("--")
        return nameLabel to valueLabel
    }

    fun validateUrl(url: String): Boolean {
        val urlRegex = Regex("(https?://)[\\w.-]+(:\\d+)*")
        if (!urlRegex.matches(url)) {
            JOptionPane.showMessageDialog(this, "Please enter a valid URL")
        }
        return url.matches(urlRegex)
    }

    fun stopResourcePolling() {
        resourceJob?.cancel()
        resourceJob = null
    }

    fun stopThreadPolling() {
        threadJob?.cancel()
        threadJob = null
    }

    suspend fun startResourcePolling() {
        try {
            while (true) {
                updateResourceMetrics()
                delay(resourceRateSpinner.value as Long)
            }
        } catch (e: CancellationException) {
            println("Resource Polling job cancelled.")
        } finally {
            resourceJob = null
        }
    }

    suspend fun startThreadPolling() {
        try {
            while (true) {
                updateThreadMetrics()
                delay(threadRateSpinner.value as Long)
            }
        } catch (e: CancellationException) {
            println("Thread Polling job cancelled.")
        } finally {
            threadJob = null
        }
    }

    fun saveConfigToJson() {
        val jsonString = json.encodeToString(LiveDiagnosticConfigFile(urlField.text, apiKeyField.text))
        val fileChooser = JFileChooser()
        fileChooser.selectedFile = File("default_output.json")
        val userSelection = fileChooser.showSaveDialog(mainPanel)
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            val fileToSave = fileChooser.selectedFile
            fileToSave.writeText(jsonString)
        }
    }

    fun readJSONFile(filePath: Path): LiveDiagnosticConfigFile {
        val jsonString = filePath.toFile().readText(Charsets.UTF_8)
        return Json.decodeFromString<LiveDiagnosticConfigFile>(jsonString)
    }

    fun executeBundleProcess() {
        val dialog = LoadingDialog()
        dialog.updateStatus(0, 10, "Generating")
        val isGenerating = genBundle().state

        if (isGenerating == "Generating" || isGenerating == "Valid") {
            dialog.updateStatus(0, 10, "Status")
            var genComplete = false

            for (i in 1..10) {
                dialog.updateStatus(i, 10, "Status")
                runBlocking { delay(1000) }
                try {
                    val status = bundleStatus()
                    if (status.state == "Valid") {
                        genComplete = true
                        dialog.updateStatus(i, 10, "Downloading")
                        break
                    }
                } catch (_: Exception) { }
                if (i == 9) {
                    dialog.updateStatus(0, 10, "Download Failed to Generate after ${10} attempts.")
                }
            }
            if (genComplete) {
                val file = downloadBundle()
                path = file.toPath()
                loadModel(path!!)

                dialog.updateStatus(0, 10, "Complete")
            }
        } else {
            dialog.updateStatus(0, 10, "Bundle Failed to Start Generating.")
        }
    }

    var currentPerformanceData: CurrentPerformanceData? = null
    var currentThreadData: CurrentThreadData? = null

    fun updateResourceMetrics() {
        currentPerformanceData = getLiveData()

        val cpuVal = currentPerformanceData?.cpu ?: 0.0
        val heapVal = (currentPerformanceData?.heapMemory ?: 0.0) / 1024.0 / 1024.0
        val nonHeapVal = (currentPerformanceData?.nonHeapMemory ?: 0.0) / 1024.0 / 1024.0

        SwingUtilities.invokeLater {
            cpuLabel.text = "%.2f%%".format(cpuVal)
            heapLabel.text = "%.2f MB".format(heapVal)
            nonHeapLabel.text = "%.2f MB".format(nonHeapVal)

//            cpuLabel.foreground = if(cpuVal > 80.0) java.awt.Color.RED else UIManager.getColor("Label.foreground")
        }
    }

    fun updateThreadMetrics() {
        currentThreadData = getThreadData()
        SwingUtilities.invokeLater {
            runningThreadsLabel.text = currentThreadData?.running?.toString() ?: "0"
            waitingThreadsLabel.text = "%.0f".format(currentThreadData?.waiting ?: 0.0)
            timedWaitingThreadsLabel.text = "%.0f".format(currentThreadData?.timedWaiting ?: 0.0)
            blockedThreadsLabel.text = "%.0f".format(currentThreadData?.blocked ?: 0.0)

//            if ((currentThreadData?.running ?: 0) > 0) {
//                runningThreadsLabel.foreground = java.awt.Color.RED
//
//            } else {
//                runningThreadsLabel.foreground = UIManager.getColor("Label.foreground")
//            }
        }
    }

    fun getThreadData(): CurrentThreadData = runAPICall<CurrentThreadData>(urlField.text + THREAD_EXECUTION_DATA, apiKeyField.text, HttpMethod.Get)

    fun getLiveData(): CurrentPerformanceData = runAPICall<CurrentPerformanceData>(urlField.text + LIVE_PERF_DATA, apiKeyField.text, HttpMethod.Get)

    fun genBundle(): BundleStatus = runAPICall<BundleStatus>(urlField.text + GENERATE_BUNDLE, apiKeyField.text, HttpMethod.Post)

    fun bundleStatus(): BundleComplete = runAPICall<BundleComplete>(urlField.text + BUNDLE_STATUS, apiKeyField.text, HttpMethod.Get)

    fun downloadBundle(): File {
        val tempFile = File.createTempFile("downloaded_diagnostic_bundle", ".tmp")
        val channel: ByteReadChannel = runAPICall(urlField.text + DOWNLOAD_BUNDLE, apiKeyField.text, HttpMethod.Get)
        val outputStream: OutputStream = tempFile.outputStream()
        try {
            runBlocking { channel.copyTo(outputStream) }
        } finally {
            outputStream.close()
        }
        return tempFile
    }

    fun loadModel(path: Path) {
        val zipFile = FileSystems.newFileSystem(path)
        provider = zipFile.provider()
        fileTree.model = ZipFileModel(zipFile)
    }

    private fun maybeAddNewTab(vararg paths: Path) {
        val pathList = paths.toList()
        val existingTab = tabStrip.tabs.find { tab -> tab.paths == pathList }
        if (existingTab == null) {
            val pathView = createView(provider!!, *paths)
            if (pathView != null) {
                pathView.putClientProperty(TABBED_PANE_TAB_CLOSABLE, pathView.closable)
                tabStrip.addTab(component = pathView, select = true)
            }
        } else {
            tabStrip.selectedComponent = existingTab
        }
    }

    companion object {
        const val GENERATE_BUNDLE = "/data/api/v1/diagnostics/bundle/generate"
        const val BUNDLE_STATUS = "/data/api/v1/diagnostics/bundle/status"
        const val DOWNLOAD_BUNDLE = "/data/api/v1/diagnostics/bundle/download"
        const val THREAD_EXECUTION_DATA = "/data/api/v1/systemPerformance/threads"
        const val HIST_PERF_DATA = "/data/api/v1/systemPerformance/charts"
        const val LIVE_PERF_DATA = "/data/api/v1/systemPerformance/currentGauges"

        inline fun <reified T> runAPICall(url: String, token: String, httpMethod: HttpMethod): T {
            val client = HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            prettyPrint = true
                            isLenient = true
                            ignoreUnknownKeys = true
                        },
                    )
                }
            }
            val result = runBlocking {
                client.request(url) {
                    method = httpMethod
                    url {
                        headers.append("Content-Type", "application/json")
                        headers.append("X-Ignition-API-Token", token)
                    }
                }.body<T>()
            }
            println(result.toString())
            client.close()
            return result
        }
    }

    class LoadingDialog : JDialog() {
        val statusLabel = JLabel("").apply {
            horizontalAlignment = SwingConstants.CENTER
        }
        val detailsLabel = JLabel("").apply {
            horizontalAlignment = SwingConstants.CENTER
        }
        val progressBar = JProgressBar().apply {
            isIndeterminate = true
            isVisible = true
        }
        val panel = JPanel(MigLayout("ins 0, fill, hidemode 3")).apply {
            border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
            add(statusLabel, "grow, wrap")
            add(detailsLabel, "grow, wrap")
            add(progressBar, "grow, wrap")
        }
        fun updateStatus(attempt: Int, total: Int, status: String) {
            when (status) {
                "Generating" -> {
                    statusLabel.text = "Generating Bundle"
                    detailsLabel.text = ""
                    progressBar.isVisible = true
                }
                "Status" -> {
                    statusLabel.text = "Checking Status${".".repeat(attempt % 4)}"
                    detailsLabel.text = "Attempt: $attempt of $total"
                    progressBar.isVisible = true
                }
                "Downloading" -> {
                    statusLabel.text = "Downloading .zip"
                    detailsLabel.text = ""
                    progressBar.isVisible = true
                }
                "Complete" -> {
                    statusLabel.text = "Download Complete!"
                    detailsLabel.text = ""
                    progressBar.isVisible = false
                }
                else -> {
                    statusLabel.text = "Download Failed!"
                    detailsLabel.text = status
                    progressBar.isVisible = false
                }
            }
            panel.revalidate()
            panel.repaint()
        }

        init {
            isVisible = true
            title = "Downloading Diagnostic Bundles"
            contentPane.add(panel)
            preferredSize = Dimension(350, 120)
            pack()
            setLocationRelativeTo(owner)
        }
    }
}

object LiveDiagnosticTool : EditorTool {
    override val serialKey: String = "livediagnostics"
    override val title: String = "Live Diagnostics"
    override val description: String = "Open tool for viewing and retrieving diagnostic data"
    override val icon: FlatSVGIcon = FlatSVGIcon("icons/bx-tachometer.svg")
    override fun open(path: Path): ToolPanel = LiveDiagnosticPanel(path)
    override fun open(): ToolPanel = LiveDiagnosticPanel(null)
    override val filter: FileFilter = FileFilter("json files", "json")
}
