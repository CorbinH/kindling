package io.github.inductiveautomation.kindling.docker.ui.editors

import io.github.inductiveautomation.kindling.docker.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.ui.ConfigSection
import io.github.inductiveautomation.kindling.utils.ColumnList
import io.github.inductiveautomation.kindling.utils.ReifiedJXTable
import io.github.inductiveautomation.kindling.utils.ReifiedListTableModel
import io.github.inductiveautomation.kindling.utils.configureCellRenderer
import java.awt.Component
import java.awt.event.MouseEvent
import java.util.EventObject
import javax.swing.AbstractCellEditor
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableModel
import net.miginfocom.swing.MigLayout

class NetworkEditor(
    initialNetworks: MutableList<String>,
    initialNetworkOptions: List<DockerNetwork>,
) : ConfigSection("Networks") {
    private val networkTable = ReifiedJXTable(DockerNetworksTableModel(initialNetworks, initialNetworkOptions)).apply {
        tableHeader.isVisible = false
        isColumnControlVisible = false
    }

    var networkOptions by networkTable.model::networkOptions

    private val networksHeader = JPanel(MigLayout("fill")).apply {
        val networkLabel = JLabel("Add/Remove, Double-click to edit")
        val addNetworkButton = JButton("+").apply {
            addActionListener {
                val newNetworkReference = networkOptions.find {
                    it.name !in networkTable.model.data
                } ?: return@addActionListener

                networkTable.model.data.add(newNetworkReference.name)
                networkTable.model.fireTableRowsInserted(
                    networkTable.model.data.size - 1,
                    networkTable.model.data.size - 1,
                )
            }

            networkTable.model.addTableModelListener {
                isEnabled = (it.source as TableModel).rowCount < networkOptions.size
            }
        }

        val removeNetworkButton = JButton("-").apply {
            isEnabled = false

            networkTable.selectionModel.addListSelectionListener {
                if (!it.valueIsAdjusting) {
                    isEnabled = !(it.source as ListSelectionModel).isSelectionEmpty
                }
            }

            addActionListener {
                val selectedItem = networkTable.selectionModel.selectedIndices.first()
                val modelIndex = networkTable.convertRowIndexToModel(selectedItem)

                networkTable.model.data.removeAt(modelIndex)
                networkTable.model.fireTableRowsDeleted(modelIndex, modelIndex)
            }
        }

        add(networkLabel, "west")
        add(addNetworkButton, "east")
        add(removeNetworkButton, "east")
    }

    init {
        add(networksHeader, "growx, wrap")
        add(networkTable, "push, grow")

        networkTable.model.addTableModelListener {
            fireConfigChange()
        }
    }
}

class DockerNetworksTableModel(
    override val data: MutableList<String>,
    networkOptions: List<DockerNetwork>,
) : ReifiedListTableModel<String>(data, DockerNetworkTableColumns) {
    override val columns = super.columns as DockerNetworkTableColumns

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return data.size < networkOptions.size
    }

    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
        if (aValue !is DockerNetwork) return

        data[rowIndex] = aValue.name
        fireTableCellUpdated(rowIndex, columnIndex)
    }

    var networkOptions: List<DockerNetwork> = networkOptions
        set(value) {
            field = value
            val names = value.map { it.name }
            val indicesToRemove = data.mapIndexedNotNull { i, n ->
                if (n !in names) i else null
            }.sortedDescending()

            if (indicesToRemove.isNotEmpty()) {
                for (index in indicesToRemove) {
                    data.removeAt(index)
                }
                fireTableDataChanged()
            }
        }

    object DockerNetworkTableColumns : ColumnList<String>() {
        @Suppress("unused")
        val Network by column(
            column = {
                cellEditor = DockerNetworkCellEditor()
            },
            value = { it }
        )
    }

    private class DockerNetworkCellEditor : AbstractCellEditor(), TableCellEditor {
        private val comboBox = JComboBox<DockerNetwork>().apply {
            configureCellRenderer { _, value, _, _, _ ->
                text = (value as DockerNetwork).name
            }
        }

        init {
            comboBox.addItemListener {
                super.fireEditingStopped()
            }
        }

        override fun isCellEditable(e: EventObject?): Boolean {
            return e is MouseEvent && e.clickCount == 2
        }

        override fun getCellEditorValue(): DockerNetwork? {
            return comboBox.selectedItem as DockerNetwork?
        }

        override fun getTableCellEditorComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            row: Int,
            column: Int,
        ): Component {
            @Suppress("unchecked_cast")
            table as ReifiedJXTable<DockerNetworksTableModel>

            val unusedOptions = table.model.networkOptions.filter {
                it.name !in table.model.data || it.name == table.model.data[row]
            }

            comboBox.model = DefaultComboBoxModel(unusedOptions.toTypedArray())
            comboBox.selectedItem = table.model.data[row]

            return comboBox
        }
    }
}
