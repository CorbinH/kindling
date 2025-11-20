package io.github.inductiveautomation.kindling.livediagnostics

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.core.EditorTool
import io.github.inductiveautomation.kindling.core.Kindling.BETA_VERSION
import io.github.inductiveautomation.kindling.core.ToolPanel
import io.github.inductiveautomation.kindling.docker.DockerDraftPanel
import io.github.inductiveautomation.kindling.docker.DockerTool
import io.github.inductiveautomation.kindling.utils.FileFilter
import io.github.inductiveautomation.kindling.utils.HorizontalSplitPane
import io.github.inductiveautomation.kindling.utils.VerticalSplitPane
import io.ktor.client.HttpClient
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import net.miginfocom.swing.MigLayout
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



class LiveDiagnosticPanel(existingFile: Path?) : ToolPanel("debug, ins 0, fill, hidemode 3") {

    override fun getToolTipText(): String? {
        return ""
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
    val APIKey = JTextField()
    val destinationUrl = JTextField()


    val saveJButton = JButton("Save").apply {
        addActionListener {

        }
    }
    val configButon = JButton("Config")

    val leftUpperPanel = JPanel(MigLayout("ins 0, fill, hidemode 3")).apply {
        add(configButon, "grow, pushx")
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
        add(JButton("Download"), "grow, wrap")
        add(resourceTree, "grow, push")
    }
    val leftPanel = VerticalSplitPane(leftUpperPanel, leftLowerPanel, resizeWeight = 0.5)
    val rightPanel = JPanel()


    val mainPanel = HorizontalSplitPane(leftPanel, rightPanel, resizeWeight = 0.25).apply {

    }

    init {

        add(mainPanel, "push, grow")

    }


    companion object {
        const val GEN_DIAG_BUNDLE = "/data/api/v1/diagnostics/bundle/generate"
        const val DOWNLOAD_BUDNLE = "/data/api/v1/diagnostics/bundle/download"
        const val THREAD_EXECUTION_DATA = "/data/api/v1/systemPerformance/threads"
        const val HIST_PERF_DATA = "/data/api/v1/systemPerformance/charts"
        const val LIVE_PERF_DATA = "/data/api/v1/systemPerformance/currentGauges"

        //todo create a post and get generic functions
        //todo generate the bundle, check the status if the staus is valid or not x number of time, and then download
        fun runAPICall(url: String, route: String, token: String, HTTPMethod: HttpMethod) {
            val client = HttpClient()
            runBlocking {
                val response: HttpResponse = client.request(url+route) {
                    method = HTTPMethod
                    url {
                        headers.append("X-Ignition-API-Token", token)
                    }
                }
                print(response.bodyAsText())
            }
        }
    }


}









object LiveDiagnosticTool : EditorTool {
    override val serialKey: String = "livediagnostics"
    override val title: String = "Live Diagnostics"
    override val description: String = "Open tool for viewing and retrieving diagnostic data"
    override val icon: FlatSVGIcon = FlatSVGIcon("icons/bx-docker.svg")

    internal val ignitionIcon: FlatSVGIcon = FlatSVGIcon("icons/Logo-Ignition-Check.svg")

    override fun open(path: Path): ToolPanel {
        return LiveDiagnosticPanel(path)
    }

    override fun open(): ToolPanel {
        return LiveDiagnosticPanel(null)
    }

    override val filter: FileFilter = FileFilter("json files", "json")
}

