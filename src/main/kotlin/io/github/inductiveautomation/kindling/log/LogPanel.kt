package io.github.inductiveautomation.kindling.log

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.core.Detail.BodyLine
import io.github.inductiveautomation.kindling.core.DetailsPane
import io.github.inductiveautomation.kindling.core.Filter
import io.github.inductiveautomation.kindling.core.FilterPanel
import io.github.inductiveautomation.kindling.core.Kindling
import io.github.inductiveautomation.kindling.core.Kindling.Preferences.Advanced.Debug
import io.github.inductiveautomation.kindling.core.Kindling.Preferences.Advanced.HyperlinkStrategy
import io.github.inductiveautomation.kindling.core.Kindling.Preferences.General.ShowFullLoggerNames
import io.github.inductiveautomation.kindling.core.Kindling.Preferences.General.UseHyperlinks
import io.github.inductiveautomation.kindling.core.LinkHandlingStrategy
import io.github.inductiveautomation.kindling.core.ToolOpeningException
import io.github.inductiveautomation.kindling.core.ToolPanel
import io.github.inductiveautomation.kindling.log.LogViewer.TimeStampFormatter
import io.github.inductiveautomation.kindling.utils.Action
import io.github.inductiveautomation.kindling.utils.EDT_SCOPE
import io.github.inductiveautomation.kindling.utils.FilterSidebar
import io.github.inductiveautomation.kindling.utils.FlatScrollPane
import io.github.inductiveautomation.kindling.utils.HorizontalSplitPane
import io.github.inductiveautomation.kindling.utils.MajorVersion
import io.github.inductiveautomation.kindling.utils.ReifiedJXTable
import io.github.inductiveautomation.kindling.utils.VerticalSplitPane
import io.github.inductiveautomation.kindling.utils.attachPopupMenu
import io.github.inductiveautomation.kindling.utils.configureCellRenderer
import io.github.inductiveautomation.kindling.utils.debounce
import io.github.inductiveautomation.kindling.utils.isSortedBy
import io.github.inductiveautomation.kindling.utils.selectedRowIndices
import io.github.inductiveautomation.kindling.utils.toBodyLine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.miginfocom.swing.MigLayout
import org.jdesktop.swingx.JXSearchField
import org.jdesktop.swingx.table.ColumnControlButton.COLUMN_CONTROL_MARKER
import java.util.Vector
import java.util.regex.PatternSyntaxException
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JSeparator
import javax.swing.JToggleButton
import javax.swing.ListSelectionModel
import javax.swing.SortOrder
import javax.swing.SwingConstants
import javax.swing.UIManager
import kotlin.time.Duration.Companion.milliseconds
import io.github.inductiveautomation.kindling.core.Detail as DetailEvent

typealias LogFilter = Filter<LogEvent>

class LogPanel(
    /**
     * Pass a **sorted** list of LogEvents, in ascending order.
     */
    private val rawData: List<LogEvent>,
) : ToolPanel("ins 0, fill, hidemode 3") {
    init {
        if (rawData.isEmpty()) {
            throw ToolOpeningException("Opening an empty log file is pointless")
        }
        if (!rawData.isSortedBy(LogEvent::timestamp)) {
            throw ToolOpeningException("Input data must be sorted by timestamp, ascending")
        }
    }

    private var invalidRegexPattern = false

    private val totalRows: Int = rawData.size

    private val header = Header()

    private val footer = Footer(totalRows)

    private val columnList =
        if (rawData.first() is SystemLogEvent) {
            SystemLogColumns
        } else {
            WrapperLogColumns
        }

    val table =
        run {
            val initialModel = createModel(rawData)
            ReifiedJXTable(initialModel, columnList).apply {
                setSortOrder(initialModel.columns.Timestamp, SortOrder.ASCENDING)
            }
        }

    private val tableScrollPane = FlatScrollPane(table)

    private val sidebar =
        FilterSidebar(
            NamePanel(rawData),
            LevelPanel(rawData),
            if (rawData.first() is SystemLogEvent) {
                @Suppress("UNCHECKED_CAST")
                MDCPanel(rawData as List<SystemLogEvent>)
            } else {
                null
            },
            if (rawData.first() is SystemLogEvent) {
                @Suppress("UNCHECKED_CAST")
                ThreadPanel(rawData as List<SystemLogEvent>)
            } else {
                null
            },
            TimePanel(
                rawData,
            ),
        )

    private val details = DetailsPane()

    private val filters: List<LogFilter> =
        buildList {
            for (panel in sidebar.filterPanels) {
                add { event ->
                    panel.filter(event) ||
                        (header.markedBehavior.selectedItem == "Always Show Marked" && event.marked)
                }
                add { event ->
                    header.markedBehavior.selectedItem != "Only Show Marked" || event.marked
                }
                add { event ->
                    val text = header.search.text
                    if (text.isNullOrEmpty()) {
                        true
                    } else if (header.markedBehavior.selectedItem == "Always Show Marked" && event.marked) {
                        true
                    } else if (header.matchRegex.isSelected) {
                        try {
                            val textRegex = text.toRegex()
                            textRegex.containsMatchIn(event.message) ||
                                textRegex.containsMatchIn(event.logger) ||
                                (if (event is SystemLogEvent) textRegex.containsMatchIn(event.thread) else false) ||
                                event.stacktrace.any { stacktrace ->
                                    textRegex.containsMatchIn(stacktrace)
                                }
                        } catch (e: Exception) {
                            if (e is PatternSyntaxException) invalidRegexPattern = true
                            header.search.postActionEvent()
                            false
                        }
                    } else {
                        val regexOptions = mutableSetOf<RegexOption>()
                        if (!header.matchCase.isSelected) regexOptions.add(RegexOption.IGNORE_CASE)
                        if (!header.matchWholeWord.isSelected) regexOptions.add(RegexOption.LITERAL)
                        val textString = if (header.matchWholeWord.isSelected) "\\b${Regex.escape(text)}\\b" else text
                        val textRegex = textString.toRegex(regexOptions)
                        textRegex.containsMatchIn(event.message) ||
                            textRegex.containsMatchIn(event.logger) ||
                            (if (event is SystemLogEvent) textRegex.containsMatchIn(event.thread) else false) ||
                            event.stacktrace.any { stacktrace ->
                                textRegex.containsMatchIn(stacktrace)
                            }
                    }
                }
            }
        }

    private val dataUpdater =
        debounce(50.milliseconds, BACKGROUND) {
            val selectedEvents = table.selectedRowIndices().map { row -> table.model[row].hashCode() }
            val filteredData =
                if (Debug.currentValue) {
                    // use a less efficient, but more debuggable, filtering sequence
                    filters.fold(rawData) { acc, logFilter ->
                        acc.filter(logFilter::filter).also {
                            println("${it.size} left after $logFilter")
                        }
                    }
                } else {
                    rawData.filter { event ->
                        filters.all { filter -> filter.filter(event) }
                    }
                }

            EDT_SCOPE.launch {
                table.apply {
                    model = createModel(filteredData)

                    selectionModel.valueIsAdjusting = true
                    model.data.forEachIndexed { index, event ->
                        if (event.hashCode() in selectedEvents) {
                            val viewIndex = convertRowIndexToView(index)
                            addRowSelectionInterval(viewIndex, viewIndex)
                        }
                    }
                    selectionModel.valueIsAdjusting = false
                }
            }
        }

    private fun updateData() = dataUpdater()

    fun reset() {
        sidebar.filterPanels.forEach(FilterPanel<LogEvent>::reset)
        header.search.text = null
    }

    @Suppress("UNCHECKED_CAST")
    private fun createModel(rawData: List<LogEvent>): LogsModel<out LogEvent> =
        when (columnList) {
            is WrapperLogColumns -> LogsModel(rawData as List<WrapperLogEvent>, columnList)
            is SystemLogColumns -> LogsModel(rawData as List<SystemLogEvent>, columnList)
        }

    override val icon: Icon? = null

    init {
        add(
            VerticalSplitPane(
                HorizontalSplitPane(
                    sidebar,
                    JPanel(MigLayout("fill")).apply {
                        add(header, "wrap, growx")
                        add(tableScrollPane, "grow, push")
                    },
                    resizeWeight = 0.1,
                ),
                details,
            ),
            "wrap, push, grow",
        )
        add(footer, "growx, spanx 2")

        table.apply {
            selectionModel.addListSelectionListener { selectionEvent ->
                if (!selectionEvent.valueIsAdjusting) {
                    selectionModel.updateDetails()
                }
                if (selectedRow in 0..<totalRows) {
                    footer.selectedRow.text = "Selected Row: " + table.convertRowIndexToView(selectedRow).toString()
                }
            }
            addPropertyChangeListener("model") {
                footer.displayedRows = model.rowCount
            }

            val clearAllMarks =
                Action("Clear all marks") {
                    model.markRows { false }
                }
            actionMap.put(
                "$COLUMN_CONTROL_MARKER.clearAllMarks",
                clearAllMarks,
            )
            attachPopupMenu { mouseEvent ->
                val rowAtPoint = rowAtPoint(mouseEvent.point)
                if (rowAtPoint != -1) {
                    addRowSelectionInterval(rowAtPoint, rowAtPoint)
                }
                val colAtPoint = columnAtPoint(mouseEvent.point)
                if (colAtPoint != -1) {
                    JPopupMenu().apply {
                        val column = model.columns[convertColumnIndexToModel(colAtPoint)]
                        val event = model[convertRowIndexToModel(rowAtPoint)]
                        for (filterPanel in sidebar.filterPanels) {
                            filterPanel.customizePopupMenu(this, column, event)
                        }

                        if (colAtPoint == model.markIndex) {
                            add(clearAllMarks)
                        }

                        if (column == SystemLogColumns.Message || column == WrapperLogColumns.Message) {
                            add(
                                Action("Mark all with same message") {
                                    model.markRows { row ->
                                        (row.message == event.message).takeIf { it }
                                    }
                                },
                            )
                        }

                        if (event.stacktrace.isNotEmpty()) {
                            add(
                                Action("Mark all with same stacktrace") {
                                    model.markRows { row ->
                                        (row.stacktrace == event.stacktrace).takeIf { it }
                                    }
                                },
                            )
                        }

                        if (column == SystemLogColumns.Thread && event is SystemLogEvent) {
                            add(
                                Action("Mark all ${event.thread} events") {
                                    model.markRows { row ->
                                        ((row as SystemLogEvent).thread == event.thread).takeIf { it }
                                    }
                                },
                            )
                        }
                    }.takeIf { it.componentCount > 0 }
                } else {
                    null
                }
            }
        }

        fun getNextMarkedIndex(): Int {
            val currentSelectionIndex = table.selectionModel.selectedIndices?.lastOrNull() ?: 0
            val markedEvents =
                table.model.data
                    .filter { it.marked }
                    .sortedBy { table.convertRowIndexToView(table.model.data.indexOf(it)) }
            val rowIndex =
                when (markedEvents.size) {
                    0 -> -1
                    1 -> table.model.data.indexOf(markedEvents.first())
                    else -> {
                        val nextMarkedEvent =
                            markedEvents.firstOrNull { event ->
                                table.convertRowIndexToView(table.model.data.indexOf(event)) > currentSelectionIndex
                            }
                        if (nextMarkedEvent == null) {
                            table.model.data.indexOf(markedEvents.first())
                        } else {
                            table.model.data.indexOf(nextMarkedEvent)
                        }
                    }
                }
            return if (rowIndex != -1) table.convertRowIndexToView(rowIndex) else -1
        }

        fun getPrevMarkedIndex(): Int {
            val currentSelectionIndex = table.selectionModel.selectedIndices?.firstOrNull() ?: 0
            val markedEvents =
                table.model.data
                    .filter { it.marked }
                    .sortedBy { table.convertRowIndexToView(table.model.data.indexOf(it)) }
            val rowIndex =
                when (markedEvents.size) {
                    0 -> -1
                    1 -> table.model.data.indexOf(markedEvents.first())
                    else -> {
                        val prevMarkedEvent =
                            markedEvents.lastOrNull { event ->
                                table.convertRowIndexToView(table.model.data.indexOf(event)) < currentSelectionIndex
                            }
                        if (prevMarkedEvent == null) {
                            table.model.data.indexOf(markedEvents.last())
                        } else {
                            table.model.data.indexOf(prevMarkedEvent)
                        }
                    }
                }
            return if (rowIndex != -1) table.convertRowIndexToView(rowIndex) else -1
        }

        header.apply {

            search.addActionListener {
                if (invalidRegexPattern) {
                    search.background = UIManager.getLookAndFeel().defaults.getColor("Component.error.focusedBorderColor")
                    search.setToolTipText("Invalid regular expression syntax!")
                    invalidRegexPattern = false
                } else {
                    search.background = UIManager.getLookAndFeel().defaults.getColor("TextField.background")
                    search.setToolTipText(null)
                    updateData()
                }
            }

            version.addActionListener {
                table.selectionModel.updateDetails()
            }
            markedBehavior.addActionListener {
                updateData()
            }
            matchCase.addActionListener {
                if (matchRegex.isSelected) matchRegex.isSelected = false
                updateData()
            }
            matchWholeWord.addActionListener {
                if (matchRegex.isSelected) matchRegex.isSelected = false
                updateData()
            }
            matchRegex.addActionListener {
                if (matchCase.isSelected) matchCase.isSelected = false
                if (matchWholeWord.isSelected) matchWholeWord.isSelected = false
                updateData()
            }

            fun updateSelection(index: Int) {
                table.selectionModel.setSelectionInterval(index, index)
                val rect = table.bounds
                rect.y = index * table.rowHeight
                rect.height = tableScrollPane.height - table.tableHeader.height - 2
                table.scrollRectToVisible(rect)
                table.updateUI()
            }
            clearMarked.addActionListener {
                table.model.markRows { false }
                updateData()
            }
            prevMarked.addActionListener {
                val prevMarkedIndex = getPrevMarkedIndex()
                if (prevMarkedIndex != -1) updateSelection(prevMarkedIndex)
            }
            nextMarked.addActionListener {
                val nextMarkedIndex = getNextMarkedIndex()
                if (nextMarkedIndex != -1) updateSelection(nextMarkedIndex)
            }
        }

        sidebar.filterPanels.forEach { filterPanel ->
            filterPanel.addFilterChangeListener(::updateData)
        }

        ShowFullLoggerNames.addChangeListener {
            table.model.fireTableDataChanged()
        }

        HyperlinkStrategy.addChangeListener {
            // if the link strategy changes, we need to rebuild all the hyperlinks
            table.selectionModel.updateDetails()
        }

        LogViewer.SelectedTimeZone.addChangeListener {
            table.model.fireTableDataChanged()
        }
    }

    private fun ListSelectionModel.updateDetails() {
        details.events =
            selectedIndices.filter { isSelectedIndex(it) }.map { table.convertRowIndexToModel(it) }.map {
                    row ->
                table.model[row]
            }.map { event ->
                DetailEvent(
                    title =
                        when (event) {
                            is SystemLogEvent -> "${TimeStampFormatter.format(event.timestamp)} ${event.thread}"
                            else -> TimeStampFormatter.format(event.timestamp)
                        },
                    message = event.message,
                    body =
                        event.stacktrace.map { element ->
                            if (UseHyperlinks.currentValue) {
                                element.toBodyLine((header.version.selectedItem as MajorVersion).version + ".0")
                            } else {
                                BodyLine(element)
                            }
                        },
                    details =
                        when (event) {
                            is SystemLogEvent -> event.mdc.associate { (key, value) -> key to value }
                            is WrapperLogEvent -> emptyMap()
                        },
                )
            }
    }

    private class Header : JPanel(MigLayout("ins 0, fill, hidemode 3")) {
        val separator = JSeparator(SwingConstants.VERTICAL)

        val prevMarked =
            JButton(FlatSVGIcon("icons/bx-arrow-up.svg").derive(Kindling.SECONDARY_ACTION_ICON_SCALE)).apply {
                toolTipText = "Jump to previous marked log event"
            }
        val nextMarked =
            JButton(FlatSVGIcon("icons/bx-arrow-down.svg").derive(Kindling.SECONDARY_ACTION_ICON_SCALE)).apply {
                toolTipText = "Jump to next marked log event"
            }
        val markedBehavior =
            JComboBox(arrayOf("Normal", "Only Show Marked", "Always Show Marked"))

        val clearMarked =
            JButton(FlatSVGIcon("icons/bxs-eraser.svg").derive(Kindling.SECONDARY_ACTION_ICON_SCALE)).apply {
                toolTipText = "Clear all visible marks"
            }

        val markedPanel =
            JPanel(MigLayout("fill, ins 0 2 0 2")).apply {
                border = BorderFactory.createTitledBorder("Marking")
                add(prevMarked)
                add(nextMarked)
                add(markedBehavior, "growy")
                add(clearMarked)
            }

        val version: JComboBox<MajorVersion> =
            JComboBox(Vector(MajorVersion.entries)).apply {
                selectedItem = MajorVersion.EightOne
                configureCellRenderer { _, value, _, _, _ ->
                    text = "${value?.version}.*"
                }
            }
        private val versionLabel = JLabel("Version")

        val versionPanel =
            JPanel(MigLayout("fill, ins 0 2 0 2")).apply {
                border = BorderFactory.createTitledBorder("Hyperlink Strategy")
                add(versionLabel)
                add(version, "growy")
            }

        val search = JXSearchField("")

        val matchCase =
            JToggleButton(FlatSVGIcon("icons/match-case.svg").derive(Kindling.SECONDARY_ACTION_ICON_SCALE))
                .apply {
                    toolTipText = "Match Case"
                }

        val matchWholeWord =
            JToggleButton(FlatSVGIcon("icons/match-whole-word.svg").derive(Kindling.SECONDARY_ACTION_ICON_SCALE))
                .apply {
                    toolTipText = "Match Whole Word"
                }

        val matchRegex =
            JToggleButton(FlatSVGIcon("icons/match-regex.svg").derive(Kindling.SECONDARY_ACTION_ICON_SCALE))
                .apply {
                    toolTipText = "Use Regular Expression"
                }

        val searchPanel =
            JPanel(MigLayout("fill, ins 0 2 0 2")).apply {
                border = BorderFactory.createTitledBorder("Search")
                add(search, "growx, growy, push")
                add(matchCase, "align right")
                add(matchWholeWord, "align right")
                add(separator, "growy, align right")
                add(matchRegex, "align right")
            }

        private fun updateVersionVisibility() {
            val isVisible = UseHyperlinks.currentValue && HyperlinkStrategy.currentValue == LinkHandlingStrategy.OpenInBrowser
            versionPanel.isVisible = isVisible
        }

        init {
            add(markedPanel, "cell 0 0, growy")
            add(versionPanel, "cell 0 0, growy")
            add(searchPanel, "cell 0 0, growx, growy")
            updateVersionVisibility()
            UseHyperlinks.addChangeListener { updateVersionVisibility() }
            HyperlinkStrategy.addChangeListener { updateVersionVisibility() }
        }
    }

    private class Footer(val totalRows: Int) : JPanel(MigLayout("ins 2 4 0 4, fill, hidemode 3")) {
        var displayedRows = totalRows
            set(value) {
                field = value
                events.text = "Showing $value of $totalRows events"
            }
        val events =
            JLabel("Showing $displayedRows of $totalRows events").apply {
                horizontalAlignment = SwingConstants.RIGHT
            }
        val selectedRow = JLabel("Selected Row: ")

        init {
            add(events, "growx")
            add(JLabel("    |    "))
            add(selectedRow, "growx, pushx")
        }
    }

    companion object {
        private val BACKGROUND = CoroutineScope(Dispatchers.Default)
    }
}
