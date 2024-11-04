package io.github.inductiveautomation.kindling.alarm

import com.formdev.flatlaf.extras.FlatSVGIcon
import com.inductiveautomation.ignition.common.alarming.AlarmEvent
import com.inductiveautomation.ignition.common.alarming.AlarmState
import io.github.inductiveautomation.kindling.alarm.model.AlarmEventColumnList
import io.github.inductiveautomation.kindling.alarm.model.PersistedAlarmInfo
import io.github.inductiveautomation.kindling.cache.AliasingObjectInputStream
import io.github.inductiveautomation.kindling.core.Detail
import io.github.inductiveautomation.kindling.core.DetailsPane
import io.github.inductiveautomation.kindling.core.Tool
import io.github.inductiveautomation.kindling.core.ToolOpeningException
import io.github.inductiveautomation.kindling.core.ToolPanel
import io.github.inductiveautomation.kindling.utils.FileFilter
import io.github.inductiveautomation.kindling.utils.ReifiedJXTable
import io.github.inductiveautomation.kindling.utils.ReifiedListTableModel
import io.github.inductiveautomation.kindling.utils.VerticalSplitPane
import java.awt.Color
import java.nio.file.Path
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import kotlin.io.path.absolutePathString
import kotlin.io.path.inputStream
import kotlin.io.path.name
import kotlinx.coroutines.selects.select
import net.miginfocom.swing.MigLayout
import org.jdesktop.swingx.JXSearchField
import org.jdesktop.swingx.decorator.ColorHighlighter

class AlarmCacheView(path: Path) : ToolPanel() {
    private val events: List<AlarmEvent> = try {
        val info = AliasingObjectInputStream(path.inputStream()) {
            put(
                "com.inductiveautomation.ignition.gateway.alarming.status.AlarmStateModel\$PersistedAlarmInfo",
                PersistedAlarmInfo::class.java,
            )
        }.readObject() as PersistedAlarmInfo

        info.data.values.flatMap { it.toList() }
    } catch (e: Exception) {
        throw ToolOpeningException("Error deserializing alarm cache. Only caches from Ignition 8.1.20+ are supported.", e)
    }

    override val icon = AlarmViewer.icon

    private val table = ReifiedJXTable(
        ReifiedListTableModel(events, AlarmEventColumnList)
    ).apply {
        alarmStateColors.forEach { (alarmState, colorPalette) ->
            val highlighter = ColorHighlighter(
                { _, rowAdapter ->
                    val rowIndex = rowAdapter.row
                    val dataIndex = convertRowIndexToModel(rowIndex)
                    val alarmEvent = model.get(dataIndex)
                    alarmEvent.state == alarmState
                },
                colorPalette.background,
                colorPalette.foreground,
            )
            this.addHighlighter(highlighter)
        }
    }

    private val countLabelText: String
        get() = "Showing ${table.rowCount} of ${events.size} alarms"

    private val alarmCountLabel = JLabel(countLabelText)

    private val searchField = JXSearchField("Search")

    private val header = JPanel(MigLayout("fill, ins 4")).apply {
        add(alarmCountLabel, "west")
        add(searchField, "east, wmin 300")
    }

    private val details = DetailsPane()

    private fun AlarmEvent.toDetail(): Detail {
        val title = "Alarm: ${this.name}"
        val message = "Notes: ${this.notes}"

        val body: List<Detail.BodyLine> = when(this.state) {
            AlarmState.ActiveUnacked -> {
                val activeData = this.activeData.values.map {
                    Detail.BodyLine("${it.property.name}: ${it.value}")
                }
                activeData
            }
            AlarmState.ClearUnacked -> {
                val clearData = this.clearedData.values.map {
                    Detail.BodyLine("${it.property.name}: ${it.value}")
                }
                clearData
            }
            AlarmState.ActiveAcked, AlarmState.ClearAcked -> {
                val ackData = this.ackData.values.map {
                    Detail.BodyLine("${it.property.name}: ${it.value}")
                }
                ackData
            }
            else -> emptyList()
        }

        return Detail(
            title = title,
            message = message,
            body = body,
        )
    }

    init {
        name = path.name
        toolTipText = path.absolutePathString()

        add(header, "north")

        val splitPane = VerticalSplitPane(
            JScrollPane(table),
            details,
        )

        add(splitPane, "grow")

        searchField.addActionListener { event ->
            val searchField = event.source as JXSearchField
            val searchText = searchField.text

            val filteredEvents = events.filter {
                searchText.lowercase() in it.name.lowercase()
            }

            val newModel = ReifiedListTableModel(filteredEvents, AlarmEventColumnList)

            table.model = newModel
            alarmCountLabel.text = countLabelText
        }

        table.selectionModel.addListSelectionListener {
            // Grab the selected items from the table
            val selectedIndices = table.selectionModel.selectedIndices.map {
                table.convertRowIndexToModel(it)
            }

            val selectedAlarmEvents: List<Detail> = selectedIndices.map {
                table.model[it].toDetail()
            }

            // Put details into details pane
            details.events = selectedAlarmEvents
        }
    }


    private data class AlarmStateColorPalette(
        val background: Color,
        val foreground: Color,
    )

    companion object {
        private val alarmStateColors = mapOf(
            AlarmState.ActiveAcked to AlarmStateColorPalette(Color(0xAB0000), Color(0xD0D0D0)),
            AlarmState.ActiveUnacked to AlarmStateColorPalette(Color(0xEC2215), Color(0xD0D0D0)),
            AlarmState.ClearAcked to AlarmStateColorPalette(Color(0xDCDCFE), Color(0x262626)),
            AlarmState.ClearUnacked to AlarmStateColorPalette(Color(0x49ABAB), Color(0x262626)),
        )
    }
}

object AlarmViewer : Tool {
    override val title: String = "Alarm Cache"
    override val description: String = "An Ignition alarm cache file. (.alarms)"
    override val icon: FlatSVGIcon = FlatSVGIcon("icons/bx-bell.svg")

    override fun open(path: Path): ToolPanel {
        return AlarmCacheView(path)
    }

    override val filter: FileFilter = FileFilter(description) {
        fileNameRegex.matches(it.name)
    }

    private val fileNameRegex = """\.?alarms_.*""".toRegex()

    override val serialKey: String = "alarm-cache"

    override val requiresHiddenFiles: Boolean = true
}
