package io.github.inductiveautomation.kindling.logback

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.core.Tool
import io.github.inductiveautomation.kindling.core.ToolPanel
import io.github.inductiveautomation.kindling.utils.FileFilter
import io.github.inductiveautomation.kindling.utils.NumericEntryField
import io.github.inductiveautomation.kindling.utils.chooseFiles
import java.io.File
import java.nio.file.Path
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.UIManager
import javax.swing.filechooser.FileNameExtensionFilter
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

    val selectedLoggersList = mutableListOf<SelectedLogger>()
    var logbackConfigManager = LogbackConfigManager(configs = LogbackConfigData())

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

//    private val loggerPreviewPanel = JPanel(MigLayout("fill, ins 10")).apply {
//        selectedLoggersList.forEach { logger ->
//            add(SelectedLoggerPreviewCard(logger), "north, growx, wrap")
//        }
//    }

    private val xmlPreviewLabel = JLabel("XML Output Preview")
    private val xmlOutputPreview = JTextArea().apply {
        isEditable = false
        text = logbackConfigManager.configString
    }

    private val saveXmlButton = JButton("Save XML file").apply {
        addActionListener {

            updateData()

            JFileChooser().apply {
                fileFilter = FileNameExtensionFilter("XML file", "xml")
                selectedFile = File("logback.xml")

                val save = showSaveDialog(null)

                if (save == JFileChooser.APPROVE_OPTION) {
                    logbackConfigManager.writeXmlFile(selectedFile.absolutePath)
                }
            }
        }
    }

    private val scrollPane = JScrollPane(xmlOutputPreview)

    private val xmlPreviewPanel = JPanel(MigLayout("fill, ins 0")).apply {
        add(xmlPreviewLabel, "north, growx, wrap")
        add(scrollPane, "push, grow, wrap")
        add(saveXmlButton, "growx")
    }

    private val previewPanel = JPanel(MigLayout("fill, ins 10")).apply {
        add(xmlPreviewPanel, "grow, push")
    }

    fun updateData() {
        println("updateData()")
        val temp = xmlOutputPreview.caretPosition

        logbackConfigManager.configs?.rootDir = RootDirectory(value = directorySelectorPanel.rootDirField.text)
        logbackConfigManager.configs?.scan = scanForChangesPanel.scanForChangesCheckbox.isSelected
        logbackConfigManager.configs?.scanPeriod = scanForChangesPanel.scanPeriod.text + " seconds"
        loggerConfigPanel.selectedLoggersPanel.components.forEachIndexed { index, selectedLoggerCard ->
            selectedLoggersList[index].level = (selectedLoggerCard as SelectedLoggerCard).loggerLevelSelector.selectedItem as String
            selectedLoggersList[index].separateOutput = selectedLoggerCard.loggerSeparateOutput.isSelected
            selectedLoggersList[index].outputFolder = selectedLoggerCard.loggerOutputFolder.text
            selectedLoggersList[index].filenamePattern = selectedLoggerCard.loggerFilenamePattern.text
            selectedLoggersList[index].maxFileSize = selectedLoggerCard.maxFileSize.textField.value as Long
            selectedLoggersList[index].totalSizeCap = selectedLoggerCard.totalSizeCap.textField.value as Long
            selectedLoggersList[index].maxDaysHistory = selectedLoggerCard.maxDays.textField.value as Long
        }
        logbackConfigManager.updateLoggerConfigs(selectedLoggersList)

        xmlOutputPreview.text = logbackConfigManager.generateXmlString()
        xmlOutputPreview.caretPosition = temp
    }

    init {
        name = path.name
        toolTipText = path.toString()

        val testObject = LogbackConfigDeserializer().getObjectFromXML(path.toString())
        println("LogbackConfigData object: $testObject")

        logbackConfigManager.configs = testObject

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
        updateData()
    }

    inner class DirectorySelectorPanel : JPanel(MigLayout("fill, ins 0")) {

        private var rootDirPath = System.getProperty("user.home")
        val rootDirField = JTextField(rootDirPath)

        private val fileChooser = JFileChooser().apply {
            addActionListener {
                if (selectedFile != null) {
                    rootDirPath = selectedFile.absolutePath
                    logbackConfigManager.configs?.rootDir = RootDirectory("ROOT", "selectedFile.absolutePath")
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

    inner class ScanForChangesPanel : JPanel(MigLayout("fill, hidemode 3, ins 0")) {

        val scanForChangesCheckbox = JCheckBox("Scan for config changes?").apply {
            addActionListener {
                this@ScanForChangesPanel.customEntryPanel.isVisible = this.isSelected
                logbackConfigManager.configs?.scan = if (this.isSelected) true else null
                logbackConfigManager.configs?.scanPeriod = if (this.isSelected) scanPeriod.value.toString() else null
            }
        }

        val scanPeriod = NumericEntryField(30).apply {
            addNumericChangeListener(::updateData)
        }

        private val customEntryPanel = JPanel(MigLayout("fill, ins 0")).apply {
            add(JLabel("Scan period (sec):"), "cell 0 0")
            add(scanPeriod, "cell 0 0, w 100")
            isVisible = false
        }

        init {
            add(scanForChangesCheckbox, "growx, split 2, wrap")
            add(customEntryPanel, "growx")
        }
    }
    inner class LoggerSelectorPanel : JPanel(MigLayout("fill, ins 0")) {

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

        val selectedLoggersPanel = JPanel(MigLayout("fill, ins 5, hidemode 0"))

        private val scrollPane = JScrollPane(selectedLoggersPanel).apply {
            border = null
            verticalScrollBar.unitIncrement = 16
        }

        fun clearAll() {
            selectedLoggersList.removeAll(selectedLoggersList)
            selectedLoggersPanel.apply {
                removeAll()
                revalidate()
                repaint()
            }
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
    }

    inner class SizeEntryField(
        label: String,
        inputValue: Long,
        unitValue: String,
    ) : JPanel(MigLayout("fill, ins 0")) {

        val textField = NumericEntryField(inputValue).apply {
            border = null
            addNumericChangeListener(::updateData)
        }

        private val unit = JTextField(unitValue).apply {
            border = null
            isEditable = false
            foreground = UIManager.getColor("TextArea.inactiveForeground")
        }

        init {
            add(
                JLabel(label).apply {
                    horizontalAlignment = SwingConstants.CENTER
                },
                "center, grow, shrinkx, wrap",
            )
            add(
                JPanel(MigLayout("fill, ins 0")).apply {
                    border = BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"))
                    add(textField, "w 50!, growy")
                    add(unit, "w 30!")
                },
                "grow, shrinkx",
            )
        }
    }

    inner class SelectedLoggerCard(logger: SelectedLogger) : JPanel(MigLayout("fill, ins 0, hidemode 3")) {

        private val closeButton = JButton(FlatSVGIcon("icons/bx-x.svg")).apply {
            border = null
            background = null
            addActionListener {
                selectedLoggersList.remove(logger)
                loggerConfigPanel.selectedLoggersPanel.components.forEachIndexed { index, component ->
                    if ((component as SelectedLoggerCard).name == logger.name) {
                        loggerConfigPanel.selectedLoggersPanel.remove(index)
                    }
                }
            }
        }

        private val loggerDescription = JLabel("<html>Description: <i>${logger.description}</i>")
        private val loggingLevels = arrayOf("OFF", "ERROR", "WARN", "INFO", "DEBUG", "TRACE", "ALL")
        val loggerLevelSelector = JComboBox(loggingLevels).apply {
            selectedItem = logger.level
        }

        val loggerOutputFolder = JTextField(logger.outputFolder).apply {
            addActionListener {
                updateData()
            }
        }

        val loggerFilenamePattern = JTextField(logger.filenamePattern).apply {
            addActionListener {
                updateData()
            }
        }

        val maxFileSize = SizeEntryField("Max File Size", logger.maxFileSize, "MB")
        val totalSizeCap = SizeEntryField("Total Size Cap", logger.totalSizeCap, "MB")
        val maxDays = SizeEntryField("Max Days", logger.maxDaysHistory, "Days")

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

            add(maxFileSize, "grow, shrinkx")
            add(totalSizeCap, "grow, shrinkx")
            add(maxDays, "grow, shrinkx")

            isVisible = false
        }
        val loggerSeparateOutput = JCheckBox("Output to separate location?").apply {
            addActionListener {
                this@SelectedLoggerCard.separateOutputOptions.isVisible = this.isSelected
            }
        }

        init {
            name = logger.name
            border = BorderFactory.createTitledBorder(logger.name)
            add(loggerLevelSelector, "w 100")
            add(loggerDescription, "growx, push")
            add(closeButton, "right, wrap")

            add(loggerSeparateOutput, "growx, span 3, wrap")
            add(separateOutputOptions, "growx, span 3")
        }
    }
//    inner class SelectedLoggerPreviewCard(logger: SelectedLogger) : JPanel(MigLayout("fill")) {
//
//        private val loggerPreviewLabel = JLabel("Preview of ${logger.name} on ${logger.level}:")
//        private val loggerPreviewBody = JTextArea("A preview of the selected logger on the selected logging level will appear here!")
//
//        init {
//            add(loggerPreviewLabel, "growx, wrap")
//            add(loggerPreviewBody, "growx")
//        }
//    }

    override val icon: Icon = LogbackEditor.icon
}

data class SelectedLogger(
    val name: String = "SelectedLogger.name",
    val description: String = " n/a",
    var level: String = "INFO",
    var separateOutput: Boolean = false,
    var outputFolder: String = "\${ROOT}\\AdditionalLogs",
    var filenamePattern: String = "${name.replace(".", "")}.%d{yyyy-MM-dd}.%i.log",
    var maxFileSize: Long = 10,
    var totalSizeCap: Long = 1000,
    var maxDaysHistory: Long = 5,
)
