package io.github.inductiveautomation.kindling.logback
import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.core.DetailsPane
import io.github.inductiveautomation.kindling.core.Tool
import io.github.inductiveautomation.kindling.core.ToolPanel
import io.github.inductiveautomation.kindling.utils.FileFilter
import io.github.inductiveautomation.kindling.utils.chooseFiles
import java.nio.file.Path
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JFileChooser
import javax.swing.JFormattedTextField
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.UIManager
import javax.swing.text.DefaultFormatterFactory
import javax.swing.text.NumberFormatter
import kotlin.io.path.name
import net.miginfocom.swing.MigLayout
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator

object LogbackEditor : Tool {
    override val title = "Logback Editor"
    override val description = "logback (.xml) files"
    override val icon = FlatSVGIcon("icons/bx-code.svg")
    private val extensions = listOf("xml")
    override val filter = FileFilter(description, extensions)
    override fun open(path: Path): ToolPanel = LogbackView(path)
}

class LogbackView(path: Path) : ToolPanel() {

    private val directorySelectorPanel = DirectorySelectorPanel()

    private val scanForChangesPanel = ScanForChangesPanel()

    private val generalConfigPanel = JPanel(MigLayout("fill, ins 0")).apply {
        add(directorySelectorPanel, "growx, wrap")
        add(scanForChangesPanel, "growx, wrap")
    }

    private val loggerConfigPanel = LoggerSelectorPanel()

    private val clearAllButton = JButton("Clear all selected loggers").apply {
        addActionListener {
            loggerConfigPanel.clearAll()
        }
    }

    private val editorPanel = JPanel(MigLayout("fill, ins 10")).apply {
        add(generalConfigPanel, "growx, wrap")
        add(loggerConfigPanel, "push, grow, wrap")
        add(clearAllButton, "growx")
    }

    private val loggerPreviewPanel = JPanel(MigLayout("fill, ins 10")).apply {
        loggerConfigPanel.selectedLoggersList.forEach { logger ->
            add(SelectedLoggerPreviewCard(logger), "north, growx, wrap")
        }
    }

    private val xmlPreviewLabel = JLabel("XML Output Preview")
    private val xmlOutputPreview = DetailsPane()
    private val saveXmlButton = JButton("Save XML file").apply {
        addActionListener {
            // Test to generate XML
            val testObject = LogbackConfigDeserializer().getObjectFromXML(path.toString())
            val test = LogbackConfigManager(testObject!!)
            println("Test instance of LogbackData: $test")
            test.apply {
                println(this.configs)

                val xmlString = this.generateXmlString(this.configs)
                println("Test XML string from data object: $xmlString")

                val testPath = "C:\\Users\\ebair\\IdeaProjects\\kindling\\src\\main\\resources\\stuff.xml"

//              this.writeXmlFile(this.configs, path.toString())
                this.writeXmlFile(this.configs, testPath)
            }

        }
    }

    private val xmlPreviewPanel = JPanel(MigLayout("fill, ins 0")).apply {
        add(xmlPreviewLabel, "north, growx, wrap")
        add(xmlOutputPreview, "push, grow, wrap")
        add(saveXmlButton, "growx")
    }

    private val previewPanel = JPanel(MigLayout("fill, ins 10")).apply {
        add(
            JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                loggerPreviewPanel,
                xmlPreviewPanel,
            ).apply {
                isOneTouchExpandable = true
                resizeWeight = 0.5
            },
            "grow, push",
        )
    }

    init {
        name = path.name
        toolTipText = path.toString()

        val testObject = LogbackConfigDeserializer().getObjectFromXML(path.toString())
        println("LogbackConfigData object: $testObject")

        add(
            JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                editorPanel,
                previewPanel,
            ).apply {
                isOneTouchExpandable = true
                resizeWeight = 0.5
            },
            "push, grow",
        )
    }

    override val icon: Icon = LogbackEditor.icon
}

data class SelectedLogger(
    val name: String = "SelectedLogger.name",
    val description: String = "SelectedLogger.description",
    val level: String = "INFO",
    val separateOutput: Boolean = false,
    val outputFolder: String = "\${ROOT}\\AdditionalLogs",
    val filenamePattern: String = "${name.replace(".", "")}.%d{yyyy-MM-dd}.%i.log",
    val maxFileSize: Long = 10,
    val totalSizeCap: Long = 1000,
    val maxDaysHistory: Int = 5,
)

class FancyTextField(inputValue: Long, unitValue: String) : JPanel(MigLayout("fill, ins 0")) {
    private val textField = JFormattedTextField(DefaultFormatterFactory(NumberFormatter()), inputValue).apply {
        border = null
    }
    private val unit = JTextField(unitValue).apply {
        border = null
        isEditable = false
        foreground = UIManager.getColor("TextArea.inactiveForeground")
    }
    init {
        border = BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"))
        add(textField, "w 50!, growy")
        add(unit, "w 30!")
    }
}

class SizeField(label: String, inputValue: Long, unit: String) : JPanel(MigLayout("fill, ins 0")) {
    private val textField = FancyTextField(inputValue, unit)
    init {
        add(
            JLabel(label).apply {
                horizontalAlignment = SwingConstants.CENTER
            },
            "center, grow, shrinkx, wrap",
        )
        add(textField, "grow, shrinkx")
    }
}

class SelectedLoggerCard(logger: SelectedLogger) : JPanel(MigLayout("fill, hidemode 3")) {

    private val loggerDescription = JLabel(logger.description)
    private val loggingLevels = arrayOf("OFF", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL")
    private val loggerLevelSelector = JComboBox(loggingLevels).apply {
        selectedItem = logger.level
    }

    private val loggerOutputFolder = JTextField(logger.outputFolder)
    private val loggerFilenamePattern = JTextField(logger.filenamePattern)

    private val separateOutputOptions = JPanel(MigLayout("fillx, ins 0")).apply {

        add(
            JPanel(MigLayout("fill, ins 0")).apply {
                add(JLabel("Output Folder:"), "cell 0 0")
                add(loggerOutputFolder, "cell 1 0 2 0, push, growx, shrinkx")
                add(JLabel("Filename Pattern:"), "cell 0 1")
                add(loggerFilenamePattern, "cell 1 1 2 1, push, growx, shrinkx")
            },
            "grow, push, shrinkx",
        )

        add(SizeField("Max File Size", logger.maxFileSize, "MB"), "grow, shrinkx")
        add(SizeField("Total Size Cap", logger.totalSizeCap, "MB"), "grow, shrinkx")
        add(SizeField("Max Days", logger.maxDaysHistory.toLong(), "Days"), "grow, shrinkx")

        isVisible = false
    }

    private val loggerSeparateOutput = JCheckBox("Output to separate location?").apply {
        addActionListener {
            this@SelectedLoggerCard.separateOutputOptions.isVisible = this.isSelected
        }
    }

    init {
        border = BorderFactory.createTitledBorder(logger.name)
        add(loggerDescription, "growx, split 2")
        add(loggerLevelSelector, "w 100, wrap")
        add(loggerSeparateOutput, "growx, wrap")
        add(separateOutputOptions, "growx")
    }
}

class SelectedLoggerPreviewCard(logger: SelectedLogger) : JPanel(MigLayout("fill")) {

    private val loggerPreviewLabel = JLabel("Preview of ${logger.name} on ${logger.level}:")
    private val loggerPreviewBody = JTextArea("A preview of the selected logger on the selected logging level will appear here!")

    init {
        add(loggerPreviewLabel, "growx, wrap")
        add(loggerPreviewBody, "growx")
    }
}

class DirectorySelectorPanel : JPanel(MigLayout("fill, ins 0")) {

    private var rootDirPath = System.getProperty("user.home")
    private val rootDirField = JTextField(rootDirPath)

    private val fileChooser = JFileChooser().apply {
        addActionListener {
            if (selectedFile != null) {
                rootDirPath = selectedFile.absolutePath
            }
        }
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    }

    private val rootDirBrowseButton = JButton("Browse").apply {
        addActionListener {
            fileChooser.chooseFiles(this@DirectorySelectorPanel)
            this@DirectorySelectorPanel.rootDirField.text = rootDirPath
        }
    }

    init {
        add(JLabel("Logs Root Directory"), "growx, wrap")
        add(rootDirField, "growx, push, split 2")
        add(rootDirBrowseButton, "w 100")
    }
}

class ScanForChangesPanel : JPanel(MigLayout("fill, hidemode 3, ins 0")) {
    private val scanForChangesCheckbox = JCheckBox("Scan for config changes?").apply {
        addActionListener {
            this@ScanForChangesPanel.customEntryPanel.isVisible = this.isSelected
        }
    }

    private val scanPeriod = JFormattedTextField(DefaultFormatterFactory(NumberFormatter()), 30)
    private val customEntryPanel = JPanel(MigLayout("fill, ins 0")).apply {
        add(JLabel("Scan period (sec):"), "growx, split 2")
        add(scanPeriod, "growx")
        isVisible = false
    }

    init {
        add(scanForChangesCheckbox, "growx, split 2, wrap")
        add(customEntryPanel, "growx")
    }
}

class LoggerSelectorPanel : JPanel(MigLayout("fill, ins 0")) {

    private val loggerItems = arrayOf(
        "perspective.clientSession",
        "projectManager",
        "alarms",
        "this.is.a.real.logger",
        "totally.a.valid.logger.name",
        "tags.execution.actors",
    )

    private val loggerComboBox = JComboBox(loggerItems).apply {
        isEditable = true
        insertItemAt("", 0)
        selectedIndex = -1
        AutoCompleteDecorator.decorate(this)
    }

    private val addButton = JButton("Add logger").apply {
        addActionListener {
            if (loggerComboBox.selectedItem != null &&
                (loggerComboBox.selectedItem as String) !in selectedLoggersList.map { logger -> logger.name }
            ) {
                selectedLoggersList.add(SelectedLogger((loggerComboBox.selectedItem as String)))
                selectedLoggersPanel.add(SelectedLoggerCard(selectedLoggersList.last()), "north, growx, shrinkx, wrap")
                revalidate()
            }
        }
    }

    // For testing
    val selectedLoggersList = mutableListOf<SelectedLogger>()

    private val selectedLoggersPanel = JPanel(MigLayout("fill, hidemode 0"))

    private val scrollPane = JScrollPane(selectedLoggersPanel).apply {
        border = null
        verticalScrollBar.unitIncrement = 16
    }

    init {
        add(JLabel("Logger Selection"), "growx, wrap")
        add(loggerComboBox, "growx, split 2")
        add(addButton, "w 100, wrap")
        add(JLabel("Selected loggers:"), "wrap")
        add(
            JPanel(MigLayout("fill, hidemode 0"))
                .apply {
                    add(scrollPane, "north, grow, push, wrap")
                    border = BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"))
                },
            "grow, push",
        )
    }

    fun clearAll() {
        selectedLoggersList.removeAll(selectedLoggersList)
        selectedLoggersPanel.apply {
            removeAll()
            revalidate()
            repaint()
        }
    }
}
