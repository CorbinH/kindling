package io.github.inductiveautomation.kindling.docker.ui.editors

import io.github.inductiveautomation.kindling.docker.model.DockerVolume
import io.github.inductiveautomation.kindling.docker.model.DockerVolumeServiceBinding
import io.github.inductiveautomation.kindling.docker.ui.ConfigSection
import io.github.inductiveautomation.kindling.utils.ColumnList
import io.github.inductiveautomation.kindling.utils.ReifiedJXTable
import io.github.inductiveautomation.kindling.utils.ReifiedListTableModel
import io.github.inductiveautomation.kindling.utils.configureCellRenderer
import java.awt.Component
import java.awt.EventQueue
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

class VolumeEditor(
    initialVolumes: MutableList<DockerVolumeServiceBinding>,
    initialVolumeOptions: List<DockerVolume>,
) : ConfigSection("Volumes") {
    private val volumesTable = ReifiedJXTable(
        DockerVolumesTableModel(initialVolumes, initialVolumeOptions)
    ).apply {
        isColumnControlVisible = false
    }

    var volumeOptions: List<DockerVolume> by volumesTable.model::volumeOptions

    private val volumeSectionHeader = JPanel(MigLayout("fill, gap 3")).apply {
        val volumeLabel = JLabel("Add/Remove")

        val addVolumeButton = JButton("+").apply {
            volumesTable.model.addTableModelListener {
                isEnabled = (it.source as TableModel).rowCount < volumeOptions.size
            }

            addActionListener {
                volumesTable.model.data.add(
                    DockerVolumeServiceBinding(
                        volumeOptions.first {
                            it.name !in volumesTable.model.data.map { binding -> binding.volumeName }
                        }.name,
                        "/usr/local/bin/ignition/data/",
                    ),
                )

                val lastIndex = volumesTable.model.data.size - 1
                volumesTable.model.fireTableRowsInserted(lastIndex, lastIndex)

                EventQueue.invokeLater {
                    volumesTable.editCellAt(lastIndex, volumesTable.model.columns[volumesTable.model.columns.bindPath])
                }
            }
        }

        val removeVolumeButton = JButton("-").apply {
            isEnabled = false

            volumesTable.selectionModel.addListSelectionListener {
                isEnabled = !(it.source as ListSelectionModel).isSelectionEmpty
            }

            addActionListener {
                val selectedIndex = volumesTable.selectionModel.selectedIndices.first()
                volumesTable.model.data.removeAt(selectedIndex)
                volumesTable.model.fireTableRowsDeleted(selectedIndex, selectedIndex)
            }
        }

        add(volumeLabel, "west")
        add(addVolumeButton, "east")
        add(removeVolumeButton, "east")
    }

    init {
        add(volumeSectionHeader, "growx, wrap")
        add(volumesTable, "push, grow")

        volumesTable.model.addTableModelListener {
            fireConfigChange()
        }
    }
}

internal class DockerVolumesTableModel(
    override val data: MutableList<DockerVolumeServiceBinding>,
    volumeOptions: List<DockerVolume>,
) : ReifiedListTableModel<DockerVolumeServiceBinding>(data, DockerVolumeTableColumns()) {
    var volumeOptions: List<DockerVolume> = volumeOptions
        set(value) {
            field = value
            val names = value.map { it.name }
            val indicesToRemove = data.mapIndexedNotNull { i, v ->
                if (v.volumeName !in names) i else null
            }.sortedDescending()

            if (indicesToRemove.isNotEmpty()) {
                for (index in indicesToRemove) {
                    data.removeAt(index)
                }
                fireTableDataChanged()
            }
        }

    override val columns = super.columns as DockerVolumeTableColumns

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        val column = columns[columnIndex]

        return when (column) {
            columns.Volume -> rowCount < volumeOptions.size
            columns.bindPath -> true
            else -> false
        }
    }

    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
        val column = columns[columnIndex]

        when (column) {
            columns.Volume -> {
                data[rowIndex].volumeName = (aValue as? DockerVolume)?.name ?: return
                fireTableCellUpdated(rowIndex, columnIndex)
            }
            columns.bindPath -> {
                if (aValue is String && aValue.isNotEmpty()) {
                    data[rowIndex].bindMount = aValue
                    fireTableCellUpdated(rowIndex, columnIndex)
                } else {
                    data.removeAt(rowIndex)
                    fireTableRowsDeleted(rowIndex, rowIndex)
                }
            }
        }
    }

    @Suppress("PropertyName")
    class DockerVolumeTableColumns : ColumnList<DockerVolumeServiceBinding>() {
        val Volume by column(
            column = {
                cellEditor = object : AbstractCellEditor(), TableCellEditor {
                    private val comboBox = JComboBox<DockerVolume>().apply {
                        configureCellRenderer { _, value, _, _, _ ->
                            text = (value as DockerVolume).name
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

                    override fun getCellEditorValue(): DockerVolume? {
                        return comboBox.selectedItem as DockerVolume?
                    }

                    override fun getTableCellEditorComponent(
                        table: JTable?,
                        value: Any?,
                        isSelected: Boolean,
                        row: Int,
                        column: Int,
                    ): Component {
                        @Suppress("unchecked_cast")
                        table as ReifiedJXTable<DockerVolumesTableModel>

                        val volumeNames = table.model.data.map { it.volumeName }
                        val unusedOptions = table.model.volumeOptions.filter {
                            it.name !in volumeNames || it.name == table.model.data[row].volumeName
                        }

                        comboBox.model = DefaultComboBoxModel(unusedOptions.toTypedArray())
                        comboBox.selectedItem = unusedOptions.find {
                            it.name == table.model.data[row].volumeName
                        }

                        return comboBox
                    }
                }
            },
            value = { it.volumeName },
        )

        val bindPath by column("Bind Path") {
            it.bindMount
        }
    }
}
