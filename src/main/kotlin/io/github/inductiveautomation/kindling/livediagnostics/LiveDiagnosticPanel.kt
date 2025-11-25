package io.github.inductiveautomation.kindling.livediagnostics

import com.formdev.flatlaf.extras.FlatSVGIcon
import com.sun.java.accessibility.util.AWTEventMonitor
import com.sun.java.accessibility.util.AWTEventMonitor.addActionListener
import io.github.inductiveautomation.kindling.core.EditorTool
import io.github.inductiveautomation.kindling.core.Kindling.BETA_VERSION
import io.github.inductiveautomation.kindling.core.ToolPanel
import io.github.inductiveautomation.kindling.docker.DockerDraftPanel
import io.github.inductiveautomation.kindling.docker.DockerTool
import io.github.inductiveautomation.kindling.utils.FileFilter
import io.github.inductiveautomation.kindling.utils.HorizontalSplitPane
import io.github.inductiveautomation.kindling.utils.VerticalSplitPane
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.miginfocom.swing.MigLayout
import java.io.File
import java.nio.file.Path
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.JTree
import javax.swing.SwingConstants
import kotlin.io.path.name
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.copyTo          // Import for efficient stream copy
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.runBlocking
import org.apache.wicket.ajax.json.JSONObject
import java.io.OutputStream
import javax.swing.JFileChooser
import javax.swing.filechooser.FileSystemView

@Serializable
data class BundleStatus(val state: String)

@Serializable
data class BundleComplete(val state: String, val fileSize: Int)

@Serializable
data class LiveDiagnosticConfigFile(val url: String, val key: String)


class LiveDiagnosticPanel(existingFile: Path?) : ToolPanel("debug, ins 0, fill, hidemode 3") {

    override fun getToolTipText(): String? {
        return "MyCoolToolTip"
    }
    override val icon: Icon = LiveDiagnosticTool.icon

    val resourceTree = JTree()
    val destinationDropdown = JComboBox(arrayOf("Test1", "test2"))
    val captureRate = JTextField().apply {
        isEnabled = false
        //set a default value for the rate and do safe input checking
    }

    val liveValuesCheckBox = JCheckBox("Live Values  ", false).apply {
        horizontalTextPosition = SwingConstants.LEFT
        addActionListener {
            captureRate.isEnabled = isSelected
        }
}
    val liveStats = JTextArea()
    var APIKey = JTextField("JacobAPIKey:qtqb-8jDJncaOMAtH6YJhX87FivhF4VMhMKtmTBKq5Y")
    var destinationUrl = JTextField("http://1.localtest.me:8188")


    val saveJButton = JButton("Save").apply {
        addActionListener {
            saveConfigToJson()
        }
    }
    val reloadButon = JButton("Reload").apply {
        addActionListener {
            runBlocking {
                executeBundleProcess()
            }
        }
    }

    val leftUpperPanel = JPanel(MigLayout("ins 0, fill, hidemode 3")).apply {
        add(reloadButon, "grow, pushx")
        add(saveJButton,"grow, wrap, pushx")
        add(JLabel("URL: "), "grow")
        add(destinationUrl, "grow, wrap, pushx")
        add(JLabel("API Key:  "), "grow")
        add(APIKey, "grow, wrap, pushx")
        add(liveStats, "push, grow, wrap, span 2")
        add(liveValuesCheckBox, "grow ,wrap")
        add(JLabel("Capture rate: "), "grow")
        add(captureRate, "grow, wrap")
        add(destinationDropdown, "grow, spanx")
    }
    val leftLowerPanel = JPanel(MigLayout("ins 0, fill, hidemode 3")).apply {
        add(JButton("Download"), "grow, wrap").apply {
            addActionListener {

            }
        }
        add(resourceTree, "grow, push")
    }
    val leftPanel = VerticalSplitPane(leftUpperPanel, leftLowerPanel, resizeWeight = 0.5)
    val rightPanel = JPanel()


    val mainPanel = HorizontalSplitPane(leftPanel, rightPanel, resizeWeight = 0.25).apply {

    }

    init {
        name = existingFile?.name ?: "Metrics"
        add(mainPanel, "push, grow")
        if (existingFile != null) {
            val configValues = readJSONFile(existingFile)
            APIKey.text = configValues.key
            destinationUrl.text = configValues.url
        }


    }


    fun saveConfigToJson() {
        val jsonString = Json{ prettyPrint = true }.encodeToString(LiveDiagnosticConfigFile(destinationUrl.text, APIKey.text))
        val fileChooser = JFileChooser()
        fileChooser.selectedFile = File("default_output.json")
        val userSelection = fileChooser.showSaveDialog(mainPanel)
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            val fileToSave = fileChooser.selectedFile
            fileToSave.writeText(jsonString)
        }
//        filePath.toFile().writeText(jsonString, Charsets.UTF_8)

    }


    fun readJSONFile(filePath: Path): LiveDiagnosticConfigFile {
        val JSONString = filePath.toFile().readText(Charsets.UTF_8)
        return Json.decodeFromString<LiveDiagnosticConfigFile>(JSONString)
    }

    suspend fun executeBundleProcess() {
        val isGenerating = genBundle().state == "Generating"
        if (isGenerating) {
            var genComplete = false

            for (i in 0 until 10) {
                try {
                    val status = bundleStatus()
                    if (status.state == "Valid") {
                        genComplete = true
                        break
                    }
                } catch (e: Exception) {

                }
                if (i == 9) {
                    println("Call Timed out after 10 seconds")
                }
                delay(1000)
            }
            println(genComplete)

            if (genComplete) {
                downloadBundle()
            }
        } else {
            println("Failed to start generating bundle")
        }

    }

    fun genBundle(): BundleStatus {
        return runAPICall<BundleStatus>(destinationUrl.text+GEN_DIAG_BUNDLE, APIKey.text, HttpMethod.Post)
    }

    fun bundleStatus(): BundleComplete {
        return runAPICall<BundleComplete>(destinationUrl.text+BUNDLE_STATUS, APIKey.text, HttpMethod.Get)
    }

    suspend fun downloadBundle(): File {
        val tempFile = File.createTempFile("downloaded_diagnostic_bundle", ".tmp")
        val channel: ByteReadChannel = runAPICall(destinationUrl.text+DOWNLOAD_BUDNLE, APIKey.text, HttpMethod.Get)
        val outputStream: OutputStream = tempFile.outputStream()
        try {
            channel.copyTo(outputStream)
        } finally {
            outputStream.close()
        }
        return tempFile
    }

    companion object {
        const val GEN_DIAG_BUNDLE = "/data/api/v1/diagnostics/bundle/generate"
        const val BUNDLE_STATUS = "/data/api/v1/diagnostics/bundle/status/"
        const val DOWNLOAD_BUDNLE = "/data/api/v1/diagnostics/bundle/download"
        const val THREAD_EXECUTION_DATA = "/data/api/v1/systemPerformance/threads"
        const val HIST_PERF_DATA = "/data/api/v1/systemPerformance/charts"
        const val LIVE_PERF_DATA = "/data/api/v1/systemPerformance/currentGauges"

        //todo create a post and get generic functions
        //todo generate the bundle, check the status if the staus is valid or not x number of time, and then download

        inline fun <reified T> runAPICall(url: String, token: String, HTTPMethod: HttpMethod): T {
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
                    method = HTTPMethod
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


}


object LiveDiagnosticTool : EditorTool {
    override val serialKey: String = "livediagnostics"
    override val title: String = "Live Diagnostics"
    override val description: String = "Open tool for viewing and retrieving diagnostic data"
    override val icon: FlatSVGIcon = FlatSVGIcon("icons/bx-docker.svg")  // Find and add an icon for the tool

    internal val ignitionIcon: FlatSVGIcon = FlatSVGIcon("icons/Logo-Ignition-Check.svg")

    override fun open(path: Path): ToolPanel {
        return LiveDiagnosticPanel(path)
    }

    override fun open(): ToolPanel {
        return LiveDiagnosticPanel(null)
    }

    override val filter: FileFilter = FileFilter("json files", "json")
}

