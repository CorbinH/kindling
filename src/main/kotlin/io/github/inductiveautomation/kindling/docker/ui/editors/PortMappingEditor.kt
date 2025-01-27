package io.github.inductiveautomation.kindling.docker.ui.editors

import io.github.inductiveautomation.kindling.docker.model.PortMapping
import io.github.inductiveautomation.kindling.docker.ui.ConfigSection
import io.github.inductiveautomation.kindling.utils.ColumnList
import io.github.inductiveautomation.kindling.utils.ReifiedJXTable
import io.github.inductiveautomation.kindling.utils.ReifiedListTableModel
import javax.swing.DefaultCellEditor
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.TableModelListener
import net.miginfocom.swing.MigLayout

class PortMappingEditor(
    initialData: MutableList<PortMapping>,
) : ConfigSection("Ports") {
    fun addTableModelListener(l: TableModelListener) {
        portMappingTable.model.addTableModelListener(l)
    }

    private val portMappingTable = ReifiedJXTable(PortMappingTableModel(initialData)).apply {
        isColumnControlVisible = false
        // Fire service model changed event from config panel
    }

    private val portMappingHeader = JPanel(MigLayout("fill")).apply {
        val portMappingLabel = JLabel("Add/Remove")

        val addPortButton = JButton("+").apply {
            addActionListener {
                portMappingTable.model.data.add(PortMapping(9088.toUShort(), 8088.toUShort()))
                portMappingTable.model.fireTableDataChanged()
            }
        }

        val removePortButton = JButton("-").apply {
            isEnabled = !portMappingTable.selectionModel.isSelectionEmpty

            portMappingTable.selectionModel.addListSelectionListener {
                isEnabled = it.firstIndex > -1
            }

            addActionListener {
                val selectedViewIndex = portMappingTable.selectionModel.selectedIndices.first()
                if (selectedViewIndex > -1) {
                    val selectedModelIndex = portMappingTable.convertRowIndexToModel(selectedViewIndex)
                    portMappingTable.model.data.removeAt(selectedModelIndex)
                    portMappingTable.model.fireTableRowsDeleted(selectedModelIndex, selectedModelIndex)
                }
            }
        }

        add(portMappingLabel, "west")
        add(addPortButton, "east")
        add(removePortButton, "east")
    }

    init {
        add(portMappingHeader, "growx, wrap")
        add(portMappingTable, "push, grow")
    }
}

internal class PortMappingTableModel(
    override val data: MutableList<PortMapping>,
) : ReifiedListTableModel<PortMapping>(data, PortMappingColumns) {
    override val columns = super.columns as PortMappingColumns

    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
        val numericValue = (aValue as? String)?.toUShortOrNull() ?: return

        when(columns[columnIndex]) {
            PortMappingColumns.Host -> data[rowIndex].hostPort = numericValue
            PortMappingColumns.Container -> data[rowIndex].containerPort = numericValue
            else -> return
        }

        fireTableCellUpdated(rowIndex, columnIndex)
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int) = true

    object PortMappingColumns : ColumnList<PortMapping>() {
        val Host by column(
            column = {
                cellEditor = DefaultCellEditor(JTextField())
            },
            value = PortMapping::hostPort
        )

        val Container by column(
            column = {
                cellEditor = DefaultCellEditor(JTextField())
            },
            value = PortMapping::containerPort,
        )
    }
}
