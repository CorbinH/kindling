package io.github.inductiveautomation.kindling.log

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.core.*
import io.github.inductiveautomation.kindling.core.Detail.BodyLine
import io.github.inductiveautomation.kindling.core.Kindling.Preferences.Advanced.HyperlinkStrategy
import io.github.inductiveautomation.kindling.core.Kindling.Preferences.General.ShowFullLoggerNames
import io.github.inductiveautomation.kindling.core.Kindling.Preferences.General.UseHyperlinks
import io.github.inductiveautomation.kindling.utils.*
import io.github.inductiveautomation.kindling.utils.Action
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.miginfocom.swing.MigLayout
import org.jdesktop.swingx.JXSearchField
import org.jdesktop.swingx.decorator.ColorHighlighter
import org.jdesktop.swingx.decorator.ComponentAdapter
import org.jdesktop.swingx.table.ColumnControlButton.COLUMN_CONTROL_MARKER
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import java.util.regex.PatternSyntaxException
import javax.swing.*
import kotlin.time.Duration.Companion.milliseconds
import io.github.inductiveautomation.kindling.core.Detail as DetailEvent

typealias LogFilter = Filter<LogEvent>

abstract class LogPanel<T : LogEvent>(
    /**
     * Pass a **sorted** list of LogEvents, in ascending order.
     */
    rawData: List<T>,
    private val columnList: LogColumnList<T>,
) : ToolPanel("ins 0, fill, hidemode 3") {

    protected val rawData: MutableList<T> = rawData.sortedBy(LogEvent::timestamp).toMutableList()

    protected var selectedData: List<T> = this.rawData
        set(value) {
            field = value.sortedBy(LogEvent::timestamp)
            footer.totalRows = value.size
            updateData()
        }

    init {
        if (this.rawData.isEmpty()) {
            throw ToolOpeningException("Opening an empty log file is pointless")
        }
    }

    protected val header = Header()
    private var invalidRegexPattern = false

    private val footer = Footer(selectedData.size)

    val table = run {
        val initialModel = createModel(this.rawData)
        ReifiedJXTable(initialModel, columnList).apply {
            setSortOrder(initialModel.columns.Timestamp, SortOrder.ASCENDING)
        }
    }

    private val tableScrollPane = FlatScrollPane(table)

    abstract val sidebar: FilterSidebar<T>

    private val sidebarContainer = JPanel(BorderLayout())

    protected fun addSidebar(sidebar: FilterSidebar<T>) {
        sidebarContainer.add(sidebar, BorderLayout.CENTER)
        filters.addAll(sidebar)
    }

    private val details = DetailsPane()

    protected val filters: MutableList<Filter<T>> = mutableListOf(
        Filter { item: T ->
            val behavior = header.markedBehavior.selectedItem as MarkedBehavior
            when (behavior) {
                MarkedBehavior.ShowAll -> true
                MarkedBehavior.OnlyMarked -> item.marked
                MarkedBehavior.OnlyUnmarked -> !item.marked
                MarkedBehavior.AlwaysShowMarked -> true
            }
        },
        Filter { event: T ->
            val text = header.search.text
            if (text.isNullOrEmpty()) {
                return@Filter true
            }

            if (header.markedBehavior.selectedItem == MarkedBehavior.AlwaysShowMarked && event.marked) {
                return@Filter true
            }

            if (header.matchRegex.isSelected) {
                try {
                    val textRegex = text.toRegex()
                    textRegex.containsMatchIn(event.message) ||
                            textRegex.containsMatchIn(event.logger) ||
                            (event is SystemLogEvent && textRegex.containsMatchIn(event.thread)) ||
                            event.stacktrace.any { stacktrace -> textRegex.containsMatchIn(stacktrace) }
                } catch (_: PatternSyntaxException) {
                    invalidRegexPattern = true
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
                        (event is SystemLogEvent && textRegex.containsMatchIn(event.thread)) ||
                        event.stacktrace.any { stacktrace -> textRegex.containsMatchIn(stacktrace) }
            }
        },
    )

    private val dataUpdater = debounce(50.milliseconds, BACKGROUND) {
        val selectedEvents = table.selectedRowIndices().map { row -> table.model[row].hashCode() }
        val behavior = header.markedBehavior.selectedItem as? MarkedBehavior ?: MarkedBehavior.ShowAll
        val filteredData = selectedData.filter { event ->
            filters.all { it.filter(event) } || (behavior == MarkedBehavior.AlwaysShowMarked && event.marked)
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

    protected fun updateData() = dataUpdater()

    fun reset() {
        sidebar.forEach { it.reset() }
        header.search.text = null
    }

    private fun createModel(rawData: List<T>): LogsModel<out T> = LogsModel(rawData, columnList)

    override val icon: Icon = LogViewer.icon

    private fun getNextMarkedIndex(forward: Boolean): Int? {
        val viewRowCount = table.rowSorter.viewRowCount

        val indicesToSearch = if (forward) {
            val minIdx = table.selectionModel.minSelectionIndex
            val startIndex = if (minIdx >= 0) minIdx + 1 else 0
            startIndex until viewRowCount
        } else {
            val maxIdx = table.selectionModel.maxSelectionIndex
            val startIndex = if (maxIdx >= 0) maxIdx - 1 else (viewRowCount - 1)
            startIndex downTo 0
        }

        return indicesToSearch.find { viewIndex ->
            val modelIndex = table.convertRowIndexToModel(viewIndex)
            table.model.data[modelIndex].marked
        }
    }

    private val markHighlighter = ColorHighlighter(
        { _, adapter: ComponentAdapter ->
            header.highlightMarked.isSelected &&
                    !table.isRowSelected(adapter.row) &&
                    table.model[table.convertRowIndexToModel(adapter.row)].marked
        },
        UIManager.getColor("Table.cellFocusColor"), // Background Color
        UIManager.getColor("Table.selectionForeground"), // Foreground Color
    )

    init {
        val rightPanel = JPanel(MigLayout("ins 0, fill"))
        rightPanel.add(header, "wrap, growx")
        rightPanel.add(tableScrollPane, "grow, push")

        @Suppress("LeakingThis")
        add(
            VerticalSplitPane(
                HorizontalSplitPane(
                    sidebarContainer,
                    rightPanel,
                    resizeWeight = 0.1,
                ),
                details,
            ),
            "wrap, push, grow",
        )
        @Suppress("LeakingThis")
        add(footer, "growx, spanx 2")

        var lastMarkedRow: Int? = null

        table.addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val viewCol = table.columnAtPoint(e.point)
                    val viewRow = table.rowAtPoint(e.point)
                    if (viewRow == -1 || viewCol == -1) return

                    val modelCol = table.convertColumnIndexToModel(viewCol)
                    if (modelCol != table.model.markIndex) return

                    val modelRow = table.convertRowIndexToModel(viewRow)

                    if (e.isShiftDown && lastMarkedRow != null) {
                        val anchor = lastMarkedRow!!
                        val range = minOf(anchor, modelRow)..maxOf(anchor, modelRow)
                        val newValue = table.model.data[modelRow].marked
                        table.model.markRows { i, _ ->
                            newValue.takeIf { i in range }
                        }
                        lastMarkedRow = null
                    } else {
                        lastMarkedRow = modelRow
                    }
                }
            },
        )

        table.selectionModel.addListSelectionListener { selectionEvent ->
            if (!selectionEvent.valueIsAdjusting) {
                table.selectionModel.updateDetails()
            }
            footer.selectedRows = table.selectionModel.minSelectionIndex + 1..table.selectionModel.maxSelectionIndex + 1
        }

        table.addPropertyChangeListener("model") {
            footer.displayedRows = table.model.rowCount
        }

        val clearAllMarks = Action("Clear all marks") {
            table.model.markRows { false }
        }

        table.actionMap.put(
            "$COLUMN_CONTROL_MARKER.clearAllMarks",
            clearAllMarks,
        )

        table.attachPopupMenu { mouseEvent ->
            val rowAtPoint = table.rowAtPoint(mouseEvent.point)
            if (rowAtPoint != -1) {
                table.addRowSelectionInterval(rowAtPoint, rowAtPoint)
            }
            val colAtPoint = table.columnAtPoint(mouseEvent.point)
            if (colAtPoint != -1) {
                JPopupMenu().apply {
                    val column = table.model.columns[table.convertColumnIndexToModel(colAtPoint)]
                    val event = table.model[table.convertRowIndexToModel(rowAtPoint)]
                    for (filterPanel in sidebar) {
                        filterPanel.customizePopupMenu(this, column, event)
                    }

                    if (colAtPoint == table.model.markIndex) {
                        add(clearAllMarks)
                    }

                    if (column == SystemLogColumns.Message || column == WrapperLogColumns.Message) {
                        add(
                            Action("Mark all with same message") {
                                table.model.markRows { row ->
                                    if (row.marked) {
                                        null
                                    } else {
                                        row.message == event.message
                                    }
                                }
                            },
                        )
                    }

                    if (event.stacktrace.isNotEmpty()) {
                        add(
                            Action("Mark all with same stacktrace") {
                                table.model.markRows { row ->
                                    (row.stacktrace == event.stacktrace).takeIf { it }
                                }
                            },
                        )
                    }

                    if (column == SystemLogColumns.Thread && event is SystemLogEvent) {
                        add(
                            Action("Mark all ${event.thread} events") {
                                table.model.markRows { row ->
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

        table.addHighlighter(markHighlighter)

        header.apply {
            search.addActionListener {
                if (invalidRegexPattern) {
                    search.background = UIManager.getColor("Component.error.focusedBorderColor")
                    search.setToolTipText("Invalid regular expression syntax!")
                    invalidRegexPattern = false
                } else {
                    search.background = UIManager.getColor("TextField.background")
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
            highlightMarked.addActionListener {
                table.repaint()
            }

            fun updateSelection(viewIndex: Int) {
                if (viewIndex < 0 || viewIndex >= table.rowCount) return
                table.selectionModel.setSelectionInterval(viewIndex, viewIndex)
                val cellRect = table.getCellRect(viewIndex, 0, true)
                table.scrollRectToVisible(cellRect)
            }
            clearMarked.addActionListener {
                table.model.markRows { false }
                updateData()
            }
            prevMarked.addActionListener {
                getNextMarkedIndex(forward = false)?.let(::updateSelection)
            }
            nextMarked.addActionListener {
                getNextMarkedIndex(forward = true)?.let(::updateSelection)
            }
        }

        ShowFullLoggerNames.addChangeListener {
            table.model.fireTableDataChanged()
        }

        HyperlinkStrategy.addChangeListener {
            table.selectionModel.updateDetails()
        }

        Timezone.Default.addChangeListener {
            table.model.fireTableDataChanged()
        }
    }

    override fun customizePopupMenu(menu: JPopupMenu) {
        menu.add(
            exportMenu { table.model },
        )
    }

    private fun ListSelectionModel.updateDetails() {
        details.events =
            selectedIndices.filter { isSelectedIndex(it) }
                .map { table.convertRowIndexToModel(it) }
                .map { row -> table.model[row] }
                .map { event ->
                    DetailEvent(
                        title = when (event) {
                            is SystemLogEvent -> "${Timezone.Default.format(event.timestamp)} ${event.thread}"
                            else -> Timezone.Default.format(event.timestamp)
                        },
                        message = event.message,
                        body = event.stacktrace.map { element ->
                            if (UseHyperlinks.currentValue) {
                                element.toBodyLine((header.version.selectedItem as MajorVersion).version + ".0")
                            } else {
                                BodyLine(element)
                            }
                        },
                        details = when (event) {
                            is SystemLogEvent -> event.mdc.associate { (key, value) -> key to value }
                            else -> emptyMap()
                        },
                    )
                }
    }

    protected inner class Header : JPanel(MigLayout("ins 0, fill, hidemode 3")) {
        val separator = JSeparator(SwingConstants.VERTICAL)

        val search = JXSearchField("")

        val matchCase = JToggleButton(FlatSVGIcon("icons/match-case.svg")).apply {
            toolTipText = "Match Case"
            isBorderPainted = false
        }

        val matchWholeWord = JToggleButton(FlatSVGIcon("icons/match-whole-word.svg")).apply {
            toolTipText = "Match Whole Word"
            isBorderPainted = false
        }

        val matchRegex = JToggleButton(FlatSVGIcon("icons/match-regex.svg")).apply {
            toolTipText = "Use Regular Expression"
            isBorderPainted = false
        }

        val version: JComboBox<MajorVersion> = JComboBox(Vector(MajorVersion.entries)).apply {
            selectedItem = MajorVersion.EightOne
            configureCellRenderer { _, value, _, _, _ ->
                text = "${value?.version}.*"
            }
        }

        private val versionLabel = JLabel("Version")

        val versionPanel = JPanel(MigLayout("fill, ins 0 2 0 2")).apply {
            border = BorderFactory.createTitledBorder("Hyperlink Strategy")
            add(versionLabel)
            add(version, "growy")
        }

        val highlightMarked = JToggleButton(FlatSVGIcon("icons/bx-highlight.svg")).apply {
            toolTipText = "Highlight all marked log events"
        }
        val clearMarked = JButton(FlatSVGIcon("icons/bxs-eraser.svg")).apply {
            toolTipText = "Clear all visible marks"
        }
        val prevMarked = JButton(FlatSVGIcon("icons/bx-arrow-up.svg")).apply {
            toolTipText = "Jump to previous marked log event"
        }
        val nextMarked = JButton(FlatSVGIcon("icons/bx-arrow-down.svg")).apply {
            toolTipText = "Jump to next marked log event"
        }

        @Suppress("EnumValuesSoftDeprecate")
        val markedBehavior = JComboBox(MarkedBehavior.values()).apply {
            selectedItem = MarkedBehavior.ShowAll
            configureCellRenderer { _, value, _, _, _ ->
                text = value?.displayName.orEmpty()
            }
        }

        private val markedPanel = JPanel(MigLayout("fill, ins 0 2 0 2")).apply {
            border = BorderFactory.createTitledBorder("Marking")
            add(prevMarked)
            add(nextMarked)
            add(markedBehavior, "growy")
            add(clearMarked)
            add(highlightMarked)
        }

        private val searchPanel = JPanel(MigLayout("fill, ins 0 2 0 2")).apply {
            border = BorderFactory.createTitledBorder("Search")
            add(search, "growx, growy, push")
            add(matchCase, "align right")
            add(matchWholeWord, "align right")
            add(separator, "growy, align right")
            add(matchRegex, "align right")
        }

        private fun updateVersionVisibility() {
            val isVisible =
                UseHyperlinks.currentValue && HyperlinkStrategy.currentValue == LinkHandlingStrategy.OpenInBrowser
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

    private class Footer(totalRows: Int) : JPanel(MigLayout("ins 2 4 0 4, fill, gap 10")) {
        var displayedRows = totalRows
            set(value) {
                field = value
                events.text = "Showing $value of $totalRows events"
            }

        var totalRows: Int = totalRows
            set(value) {
                field = value
                events.text = "Showing $displayedRows of $value events"
            }

        var selectedRows: IntRange? = null
            set(value) {
                field = value
                selectedRow.text = "Selected Row(s): $value"
            }

        private val events = JLabel("Showing $displayedRows of $totalRows events")
        private val selectedRow = JLabel(
            "Selected Row(s): ${selectedRows?.joinToString(prefix = "[", postfix = "]") ?: "None"}",
        )

        init {
            add(events, "growx")
            add(JSeparator(SwingConstants.VERTICAL), "h 10!")
            add(selectedRow, "growx, pushx")
        }
    }

    companion object {
        private val BACKGROUND = CoroutineScope(Dispatchers.Default)
    }

    protected enum class MarkedBehavior(val displayName: String) {
        ShowAll("Show All Events"),
        OnlyMarked("Only Show Marked"),
        OnlyUnmarked("Only Show Unmarked"),
        AlwaysShowMarked("Always Show Marked"),
    }
}