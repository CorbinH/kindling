package io.github.paulgriffith.kindling.log

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.paulgriffith.kindling.utils.Action
import io.github.paulgriffith.kindling.utils.FilterComparator
import io.github.paulgriffith.kindling.utils.FilterList
import io.github.paulgriffith.kindling.utils.FilterModel
import io.github.paulgriffith.kindling.utils.FlatScrollPane
import io.github.paulgriffith.kindling.utils.listCellRenderer
import net.miginfocom.swing.MigLayout
import javax.swing.ButtonGroup
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JToggleButton
class LogNameList(private val emptyLabel: String) : FilterList(emptyLabel) {

    var isShowFullLoggerNames = false
        set(value) {
            field = value
            updateCellRenderer()
        }
    init {
        updateCellRenderer()
    }

    private fun updateCellRenderer() {
        cellRenderer = listCellRenderer<Any?> { _, value, _, _, _ ->
            text = when (value) {
                is String -> {
                    val name = if (isShowFullLoggerNames) {
                        value
                    } else {
                        value.substringAfterLast(".")
                    }
                    "$name - ${model.rawData[value]} (${percentages.getValue(value)})"
                }
                null -> "$emptyLabel - ${model.rawData[null]} (${percentages.getValue(null)})"
                else -> value.toString()
            }
        }
    }
}

class LoggerNamesPanel( var events: List<LogEvent>) : JPanel(MigLayout("ins 0, fill")) {
    var loggerNamesList = LogNameList("")
    private val flatScrollPane = FlatScrollPane(
            loggerNamesList.apply {
                model = FilterModel(events.groupingBy(LogEvent::logger).eachCount())
                selectAll()
            }
    )
    private val sortButtons = ButtonGroup()
    private val naturalAsc = JToggleButton(
        configSortAction(NATURAL_SORT_ASCENDING, "Sort A-Z") {
            when (loggerNamesList.isShowFullLoggerNames) {
                true -> FilterModel.byNameAsc
                false -> bySubNameAsc
            }
        },
    )
    private val naturalDesc = JToggleButton(
        configSortAction(NATURAL_SORT_DESCENDING, "Sort Z-A") {
            when (loggerNamesList.isShowFullLoggerNames) {
                true -> FilterModel.byNameDesc
                false -> bySubNameDesc
            }
        }
    )
    private val countDesc = JToggleButton(
        configSortAction(
            icon = NUMERIC_SORT_DESCENDING,
            tooltip = "Sort by Count",
            comparatorProvider = FilterModel::byCountDesc,
        )
    )
    private val countAsc = JToggleButton(
        configSortAction(
            icon = NUMERIC_SORT_ASCENDING,
            tooltip = "Sort by Count (ascending)",
            comparatorProvider = FilterModel::byCountAsc,
        )
    )
    private fun configSortAction(icon: FlatSVGIcon, tooltip: String, comparatorProvider: () -> FilterComparator): Action {
        return Action(description = tooltip, icon = icon) {
            loggerNamesList.updateComparator(comparatorProvider())
        }
    }

    init {
        add(JLabel("Logger Name Filter"), "align center, wrap")
        listOf(
                naturalAsc,
                naturalDesc,
                countAsc,
                countDesc,
        ).forEach { sortButton ->
            sortButtons.add(sortButton)
            add(sortButton, "cell 0 1")
        }
        sortButtons.setSelected(countDesc.model, true)
        add(flatScrollPane, "newline, push, grow")
    }

    companion object {
        private val bySubNameAsc: FilterComparator = compareBy(nullsFirst(String.CASE_INSENSITIVE_ORDER)) { entry ->
            entry.key?.substringAfterLast(".")
        }
        private val bySubNameDesc = bySubNameAsc.reversed()

        private val NATURAL_SORT_ASCENDING = FlatSVGIcon("icons/bx-sort-a-z.svg")
        private val NATURAL_SORT_DESCENDING = FlatSVGIcon("icons/bx-sort-z-a.svg")
        private val NUMERIC_SORT_ASCENDING = FlatSVGIcon("icons/bx-sort-up.svg")
        private val NUMERIC_SORT_DESCENDING = FlatSVGIcon("icons/bx-sort-down.svg")
    }
}
