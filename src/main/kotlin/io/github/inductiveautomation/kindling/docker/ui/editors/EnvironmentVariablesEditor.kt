package io.github.inductiveautomation.kindling.docker.ui.editors

import io.github.inductiveautomation.kindling.docker.model.EnvironmentVariable
import io.github.inductiveautomation.kindling.docker.ui.ConfigSection
import io.github.inductiveautomation.kindling.utils.ColumnList
import io.github.inductiveautomation.kindling.utils.FlatScrollPane
import io.github.inductiveautomation.kindling.utils.ReifiedJXTable
import io.github.inductiveautomation.kindling.utils.ReifiedTableModel
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel
import net.miginfocom.swing.MigLayout
import org.jdesktop.swingx.JXTextArea

class EnvironmentVariablesEditor(
    private val envVariables: MutableMap<String, String>,
) : ConfigSection("Environment Variables") {
    private val envVariablesTable = ReifiedJXTable(ReifiedMapTableModel(envVariables)).apply {
        isColumnControlVisible = false
        isSortable = false
    }

    private val envSectionHeader = JPanel(MigLayout("fill")).apply {
        val envVariableLabel = JLabel("Add/Remove")
        val keyEntry = JXTextArea("Key")
        val valueEntry = JXTextArea("Value")

        val addEnvButton = JButton("+").apply {
            addActionListener {
                if (!keyEntry.text.isNullOrEmpty() && !valueEntry.text.isNullOrEmpty()) {
                    envVariables[keyEntry.text] = valueEntry.text
                    envVariablesTable.model.fireTableDataChanged()
                }
            }
        }

        val removeEnvButton = JButton("-").apply {
            isEnabled = !envVariablesTable.selectionModel.isSelectionEmpty

            envVariablesTable.selectionModel.addListSelectionListener {
                isEnabled = !(it.source as ListSelectionModel).isSelectionEmpty
            }

            addActionListener {
                val entries = envVariables.keys.toList()
                val toRemove = envVariablesTable.selectionModel.selectedIndices.map {
                    entries[it]
                }
                toRemove.forEach { envVariables.remove(it) }
                envVariablesTable.model.fireTableDataChanged()
            }
        }

        add(envVariableLabel, "west")
        add(keyEntry, "grow, sg")
        add(valueEntry, "grow, sg")
        add(addEnvButton, "east")
        add(removeEnvButton, "east")
    }

    init {
        add(envSectionHeader, "growx, spanx")
        add(FlatScrollPane(envVariablesTable), "push, grow")

        envVariablesTable.model.addTableModelListener {
            fireConfigChange()
        }
    }
}

class ReifiedMapTableModel(
    private val data: MutableMap<String, String>,
) : AbstractTableModel(), ReifiedTableModel<EnvironmentVariable> {
    override fun getRowCount() = data.size
    override fun getColumnCount() = 2
    override fun getColumnClass(columnIndex: Int) = columns[columnIndex].clazz
    override fun getColumnName(columnIndex: Int) = columns[columnIndex].header

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val pair = data.entries.toList().get(rowIndex).toPair()
        return when (columnIndex) {
            0 -> pair.first
            1 -> pair.second
            else -> error("Column index $columnIndex out of bounds. Should be 0 or 1.")
        }
    }

    @Suppress("unused")
    override val columns = object : ColumnList<EnvironmentVariable>() {
        val Key by column { it.first }
        val Value by column { it.second }
    }
}
