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
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.miginfocom.swing.MigLayout
import java.io.File
import java.nio.file.Path
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingConstants
import kotlin.io.path.name
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.OutputStream
import java.nio.file.FileSystems
import java.nio.file.spi.FileSystemProvider
import javax.swing.BorderFactory
import javax.swing.JDialog
import javax.swing.JFileChooser
import javax.swing.JProgressBar
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.swing.SwingUtilities
import javax.swing.UIManager
import kotlin.coroutines.cancellation.CancellationException

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

    override fun getToolTipText(): String {
        return "Live Diagnostic Tool"
    }
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
            if (resourceJob == null) {
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
            if (threadJob == null) {
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
                executeBundleProcess()
                getLiveData()
            }
        }
    }

    val leftUpperPanel = JPanel(MigLayout("ins 3, fill, hidemode 3")).apply {
        border = BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"))
        add(JPanel(MigLayout("fill")).apply {
            add(JLabel("URL: "))
            add(urlField, "grow, wrap, pushx")
            add(JLabel("API Key:  "))
            add(apiKeyField, "grow, wrap, pushx")
        }, "grow, wrap")

        add(JPanel(MigLayout("fill")).apply{
            add(downloadBundleButton, "grow, pushx")
            add(saveButton,"grow, pushx")
        }, "grow, wrap")
        add(resourceStats, "push, grow, wrap, h 100!")
        add(threadStats, "push, grow, wrap, h 100!")
        add(JPanel(MigLayout("fill")).apply {
            add(JLabel("Live Resource Capture"), "grow")
            add(JLabel("Capture Rate: ").apply {
                horizontalAlignment = SwingConstants.RIGHT
            }, "grow")
            add(resourceRateSpinner, "grow")
            add(liveResourceMetricsButton, "grow, wrap")
            add(JLabel("Live Thread Capture"), "grow")
            add(JLabel("Capture Rate: ").apply {
                horizontalAlignment = SwingConstants.RIGHT
            }, "grow")
            add(threadRateSpinner, "grow")
            add(liveThreadsButton, "grow, wrap")
        },"grow, wrap")

    }
    val leftLowerPanel = JPanel(MigLayout("ins 3, fill, hidemode 3")).apply {
        border = BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"))
        add(fileTree, "grow, push, wrap")
        add(JButton("Save Bundle").apply {
            addActionListener {

            }
        }, "grow")
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

    private val json = Json {prettyPrint = true}
    private var isActive = false

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

            for (i in 1 .. 10) {
                dialog.updateStatus(i, 10, "Status")
                runBlocking {delay(1000)}
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

        val cpuPercentage = currentPerformanceData?.cpu?.let { "%.2f".format(it) } ?: "0.00"
        val heapMemory = currentPerformanceData?.heapMemory?.let { "%.2f".format(it / 1024.0 / 1024.0) } ?: "0.00"
        val nonHeapMemory = currentPerformanceData?.nonHeapMemory?.let { "%.2f".format(it / 1024.0 / 1024.0) } ?: "0.00"

        SwingUtilities.invokeLater {
            resourceStats.text = """
            Resource Data
            CPU: $cpuPercentage%
            HeapMemory: $heapMemory MB
            NonHeapMemory: $nonHeapMemory MB
        """.trimIndent()
        }
    }

    fun updateThreadMetrics() {
        currentThreadData = getThreadData()

        val running = currentThreadData?.running ?: 0
        val waiting = currentThreadData?.waiting ?: 0
        val timedWaiting = currentThreadData?.timedWaiting ?: 0
        val blocked = currentThreadData?.blocked ?: 0

        SwingUtilities.invokeLater {
            threadStats.text = """
            Threading Data
            Running: $running
            Waiting: $waiting
            TimedWaiting: $timedWaiting
            Blocked: $blocked
        """.trimIndent()
        }
    }

    fun getThreadData(): CurrentThreadData {
        return runAPICall<CurrentThreadData>(urlField.text+THREAD_EXECUTION_DATA, apiKeyField.text, HttpMethod.Get)
    }

    fun getLiveData(): CurrentPerformanceData {
        return runAPICall<CurrentPerformanceData>(urlField.text+LIVE_PERF_DATA, apiKeyField.text, HttpMethod.Get)

    }

    fun genBundle(): BundleStatus {
        return runAPICall<BundleStatus>(urlField.text+GENERATE_BUNDLE, apiKeyField.text, HttpMethod.Post)
    }

    fun bundleStatus(): BundleComplete {
        return runAPICall<BundleComplete>(urlField.text+BUNDLE_STATUS, apiKeyField.text, HttpMethod.Get)
    }

    fun downloadBundle(): File {
        val tempFile = File.createTempFile("downloaded_diagnostic_bundle", ".tmp")
        val channel: ByteReadChannel = runAPICall(urlField.text+DOWNLOAD_BUNDLE, apiKeyField.text, HttpMethod.Get)
        val outputStream: OutputStream = tempFile.outputStream()
        try {
            runBlocking {channel.copyTo(outputStream)}
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
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                        })
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

    class LoadingDialog(): JDialog() {
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

object LiveDiagnosticTool: EditorTool {
    override val serialKey: String = "livediagnostics"
    override val title: String = "Live Diagnostics"
    override val description: String = "Open tool for viewing and retrieving diagnostic data"
    override val icon: FlatSVGIcon = FlatSVGIcon("icons/bx-tachometer.svg")
    override fun open(path: Path): ToolPanel {
        return LiveDiagnosticPanel(path)
    }
    override fun open(): ToolPanel {
        return LiveDiagnosticPanel(null)
    }
    override val filter: FileFilter = FileFilter("json files", "json")
}

