package io.github.inductiveautomation.kindling.docker.ui.editors

import io.github.inductiveautomation.kindling.docker.model.CLI_REGEX
import io.github.inductiveautomation.kindling.docker.model.CliArgument
import io.github.inductiveautomation.kindling.docker.model.GatewayCommandLineArgument
import io.github.inductiveautomation.kindling.docker.ui.ConfigSection
import io.github.inductiveautomation.kindling.utils.ColumnList
import io.github.inductiveautomation.kindling.utils.FlatScrollPane
import io.github.inductiveautomation.kindling.utils.RegexInputVerifier
import io.github.inductiveautomation.kindling.utils.ReifiedJXTable
import io.github.inductiveautomation.kindling.utils.ReifiedListTableModel
import io.github.inductiveautomation.kindling.utils.configureCellRenderer
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import net.miginfocom.swing.MigLayout

class GatewayCliArgEditor(
    private val args: MutableList<CliArgument>,
) : ConfigSection("Command Line Arguments") {
    private val cliArgumentTable = ReifiedJXTable(GatewayCliTableModel(args)).apply {
        isColumnControlVisible = false
        isSortable = false
        tableHeader = null
    }

    private val cliSectionHeader = JPanel(MigLayout("fill")).apply {
        val cliLabel = JLabel("Add/Remove")
        val cliEntry = JComboBox<String>().apply {
            isEditable = true

            inputVerifier = RegexInputVerifier(CLI_REGEX, true)

            configureCellRenderer { _, value, _, _, _ ->
                if (value != null) {
                    text = "${flags[value]?.displayName} ($value)"
                }
            }

            model = DefaultComboBoxModel(flags.keys.toTypedArray())
            selectedIndex = -1
        }

        val addCliButton = JButton("+").apply {
            addActionListener {
                if (cliEntry.inputVerifier.verify(cliEntry)) {
                    val s = cliArgumentTable.model.getRowCount()

                    val newArgs = CLI_REGEX.findAll(cliEntry.editor.item as String).map {
                        it.value
                    }.filter {
                        it !in args
                    }.toList()

                    cliArgumentTable.model.data.addAll(newArgs)

                    cliEntry.selectedIndex = -1
                    cliArgumentTable.model.fireTableRowsInserted(s, s)
                }
            }
        }

        val removeCliButton = JButton("-").apply {
            isEnabled = false

            cliArgumentTable.selectionModel.addListSelectionListener {
                isEnabled = !(it.source as ListSelectionModel).isSelectionEmpty
            }

            addActionListener {
                val iter = cliArgumentTable.model.data.iterator()

                val items = cliArgumentTable.selectionModel.selectedIndices.map {
                    val modelIndex = cliArgumentTable.convertRowIndexToModel(it)
                    cliArgumentTable.model.data[modelIndex]
                }.toMutableList()

                while (iter.hasNext() && items.isNotEmpty()) {
                    val next = iter.next()
                    if (next in items) {
                        items.remove(next)
                        iter.remove()
                    }
                }

                cliArgumentTable.model.fireTableDataChanged()
            }
        }

        add(cliLabel, "west")
        add(cliEntry, "grow")
        add(addCliButton, "east")
        add(removeCliButton, "east")
    }

    init {
        add(cliSectionHeader, "growx, spanx")
        add(FlatScrollPane(cliArgumentTable), "push, grow")

        cliArgumentTable.model.addTableModelListener {
            fireConfigChange()
        }
    }

    private class GatewayCliTableModel(
        override val data: MutableList<CliArgument>
    ) : ReifiedListTableModel<CliArgument>(data, GatewayCliColumns) {
        override fun isCellEditable(rowIndex: Int, columnIndex: Int) = true

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            require(columnIndex in 0..<columnCount) {
                "Invalid column index: $columnIndex for column count $columnCount."
            }

            require(rowIndex in 0..<rowCount) {
                "invalid row index: $rowIndex for row count $rowCount."
            }

            data[rowIndex] = aValue as? CliArgument ?: return

            fireTableRowsUpdated(rowIndex, rowIndex)
        }
    }

    private object GatewayCliColumns : ColumnList<CliArgument>() {
        @Suppress("unused")
        val Argument by column { it }
    }

    companion object {
        private val flags = GatewayCommandLineArgument.entries.associateBy { it.flag }
    }
}
