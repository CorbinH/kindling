package io.github.inductiveautomation.kindling.docker.ui.editors

import io.github.inductiveautomation.kindling.docker.model.DockerEnvironmentVariableDefinition
import io.github.inductiveautomation.kindling.docker.model.DockerEnvironmentVariableDefinition.Companion.getConnectionVariableFromInstance
import io.github.inductiveautomation.kindling.docker.model.DockerEnvironmentVariableDefinition.Companion.isConnectionVariable
import io.github.inductiveautomation.kindling.docker.model.DockerEnvironmentVariableDefinition.Companion.toYamlString
import io.github.inductiveautomation.kindling.docker.model.IgnitionVersionComparator
import io.github.inductiveautomation.kindling.docker.model.MSSQLStaticDefinition
import io.github.inductiveautomation.kindling.docker.ui.ConfigSection
import io.github.inductiveautomation.kindling.utils.ColorHighlighter
import io.github.inductiveautomation.kindling.utils.Column
import io.github.inductiveautomation.kindling.utils.ColumnList
import io.github.inductiveautomation.kindling.utils.NoSelectionModel
import io.github.inductiveautomation.kindling.utils.ReifiedJXTable
import io.github.inductiveautomation.kindling.utils.ReifiedMapTableModel
import io.github.inductiveautomation.kindling.utils.ReifiedTableModel
import io.github.inductiveautomation.kindling.utils.StringPairColumns
import io.github.inductiveautomation.kindling.utils.configureCellRenderer
import net.miginfocom.swing.MigLayout
import org.jdesktop.swingx.JXTextArea
import java.awt.Color
import java.awt.Component
import java.awt.event.MouseEvent
import java.util.EventObject
import javax.swing.AbstractCellEditor
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.RowFilter
import javax.swing.UIManager
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellEditor
import kotlin.collections.first
import kotlin.collections.isNotEmpty
import kotlin.collections.toTypedArray
import kotlin.properties.Delegates

class MSSQLEnvVariablesEditor(
    private val data: MutableMap<String, String>,
    version: String,
) : ConfigSection("Environment Variables", "fill, ins 0, gap 4") {
    /**
     * Divided into 3 sections: Pre-canned variables, variables from connection settings, and custom variables.
     */
    private val mssqlSettingsTable = ReifiedJXTable(MSSQLEnvironmentVariableTableModel(data, version)).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
    }
    var version: String by mssqlSettingsTable.model::version
    private val mssqlSettingsLabel = JLabel("MSSQL Environment Variables")
    private val addButton = JButton("+").apply {
        addActionListener {
            val s = mssqlSettingsTable.model.rowCount

            val currentVars = mssqlSettingsTable.model.staticVariableData.map { it.first }
            val newEntry = MSSQLStaticDefinition.entries.find {
                it !in currentVars
            }

            if (newEntry != null) {
                val newValue = defaultOverrides[newEntry]

                if (newValue == null) {
                    mssqlSettingsTable.model.staticVariableData.add(Pair(newEntry, newEntry.default))
                } else {
                    mssqlSettingsTable.model.staticVariableData.add(Pair(newEntry, newValue))
                    data[newEntry.name] = newValue
                }

                mssqlSettingsTable.model.fireTableRowsInserted(s, s)
            }
        }

        mssqlSettingsTable.model.addTableModelListener {
            isEnabled = (it.source as MSSQLEnvironmentVariableTableModel).getUnusedOptions().isNotEmpty()
        }
    }
    private val removeButton = JButton("-").apply {
        isEnabled = false
        mssqlSettingsTable.selectionModel.addListSelectionListener {
            isEnabled = !(it.source as ListSelectionModel).isSelectionEmpty
        }

        addActionListener {
            val index = mssqlSettingsTable.selectionModel.selectedIndices.first()
            val modelIndex = mssqlSettingsTable.convertRowIndexToModel(index)

            val removed = mssqlSettingsTable.model.staticVariableData.removeAt(modelIndex)
            data.remove(removed.first.name)
            mssqlSettingsTable.model.fireTableDataChanged()
        }
    }

    private val customSettingsLabel = JLabel("Custom Environment Variables")
    private val customVariablesTable = ReifiedJXTable(ReifiedMapTableModel(data, StringPairColumns)).apply {
        isColumnControlVisible = false
        isSortable = false
        setRowFilter(
            object : RowFilter<ReifiedMapTableModel<String>, Int>() {
                override fun include(entry: Entry<out ReifiedMapTableModel<String>, out Int>?): Boolean {
                    val k = entry?.model?.getValueAt(entry.identifier, 0) as String
                    val connectionName = k.getConnectionVariableFromInstance()
                    return DockerEnvironmentVariableDefinition.variableDefinitionsByName[k] == null && connectionName == null
                }
            },
        )
    }

    private val customVariablesHeader = JPanel(MigLayout("fill")).apply {
        val envVariableLabel = JLabel("Add/Remove")
        val keyEntry = JXTextArea("Key")
        val valueEntry = JXTextArea("Value")

        val addEnvButton = JButton("+").apply {
            addActionListener {
                if (!keyEntry.text.isNullOrEmpty() && !valueEntry.text.isNullOrEmpty()) {
                    data[keyEntry.text] = valueEntry.text
                    customVariablesTable.model.fireTableDataChanged()
                }
            }
        }

        val removeEnvButton = JButton("-").apply {
            isEnabled = !customVariablesTable.selectionModel.isSelectionEmpty

            customVariablesTable.selectionModel.addListSelectionListener {
                isEnabled = !(it.source as ListSelectionModel).isSelectionEmpty
            }

            addActionListener {
                val entries = data.keys.toList()
                val toRemove = customVariablesTable.selectionModel.selectedIndices.map {
                    entries[customVariablesTable.convertRowIndexToModel(it)]
                }
                toRemove.forEach { data.remove(it) }
                customVariablesTable.model.fireTableDataChanged()
            }
        }

        add(envVariableLabel, "west")
        add(keyEntry, "grow, sg")
        add(valueEntry, "grow, sg")
        add(addEnvButton, "east")
        add(removeEnvButton, "east")
    }

    private val connectionSettingsLabel = JLabel("Variables from connection settings")
    private val connectionVariablesList = JList<String>().apply {
        selectionModel = NoSelectionModel()
    }

    init {
        add(mssqlSettingsLabel, "growx")
        add(removeButton)
        add(addButton, "wrap")
        add(mssqlSettingsTable, "push, grow, span, sg")
        add(customSettingsLabel, "growx, spanx")
        add(customVariablesHeader, "growx, spanx")
        add(customVariablesTable, "push, grow, span, sg")
        add(connectionSettingsLabel, "growx, spanx")
        add(connectionVariablesList, "push, grow, span, sg")

        updateData()

        mssqlSettingsTable.model.addTableModelListener {
            fireConfigChange()
        }

        customVariablesTable.model.addTableModelListener {
            fireConfigChange()
        }

        mssqlSettingsTable.addHighlighter(
            ColorHighlighter(UIManager.getColor("Actions.Red"), Color.WHITE) { _, adapter ->
                val modelRow = mssqlSettingsTable.convertRowIndexToModel(adapter.row)
                !mssqlSettingsTable.model.meetsMinimumVersion(modelRow)
            },
        )
    }

    override fun updateData() {
        connectionVariablesList.model = DefaultListModel<String>().apply {
            addAll(
                data.entries.map {
                    it.toPair()
                }.filter {
                    it.isConnectionVariable()
                }.map {
                    it.toYamlString()
                },
            )
        }
    }

    companion object {
        internal val defaultOverrides: Map<MSSQLStaticDefinition, String> = mapOf(
            MSSQLStaticDefinition.ACCEPT_EULA to "Y",
            MSSQLStaticDefinition.MSSQL_DATABASE to "",
            MSSQLStaticDefinition.MSSQL_USER to "",
            MSSQLStaticDefinition.MSSQL_PID to "Developer",
        )
    }
}

class MSSQLEnvironmentVariableTableModel(
    private val dataSource: MutableMap<String, String>,
    version: String,
) : AbstractTableModel(), ReifiedTableModel<Pair<MSSQLStaticDefinition, String>> {
    var version by Delegates.observable(version) { _, _, _ ->
        fireTableDataChanged()
    }

    override fun getRowCount() = staticVariableData.size
    override fun getColumnCount() = size
    override fun getColumnClass(columnIndex: Int) = columns[columnIndex].clazz
    override fun getColumnName(columnIndex: Int) = columns[columnIndex].header

    private val allVariables = MSSQLStaticDefinition.entries.toHashSet()

    /*
     * The table's actual data. Since maps aren't ordered, we need to copy the data here and keep it
     * in sync with the map data.
     */

    internal val staticVariableData: MutableList<Pair<MSSQLStaticDefinition, String>> = dataSource.filter {
        DockerEnvironmentVariableDefinition.variableDefinitionsByName.containsKey(it.key)
    }.map { MSSQLStaticDefinition.valueOf(it.key) to it.value }.toMutableList()

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return columns[columnIndex] == Value || getUnusedOptions().isNotEmpty()
    }

    fun meetsMinimumVersion(rowIndex: Int): Boolean {
        return IgnitionVersionComparator.compare(
            staticVariableData[rowIndex].first.minimumVersion,
            version,
        ) <= 0
    }

    fun getUnusedOptions(forRow: Int? = null): List<MSSQLStaticDefinition> {
        val currentKeys = staticVariableData.map { it.first }
        val value = forRow?.let { getValueAt(it, 0) }

        return allVariables.filter { it !in currentKeys || it == value }.sortedBy {
            it.name
        }
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        require(columnIndex in 0..1) { "Column index $columnIndex out of bounds. Should be 0 or 1." }
        return columns[columnIndex].getValue(staticVariableData[rowIndex])
    }

    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
        if (columnIndex == 0) {
            aValue as MSSQLStaticDefinition
            val currentValue = getValueAt(rowIndex, columnIndex) as MSSQLStaticDefinition

            dataSource.remove(currentValue.name)

            val newValue = MSSQLEnvVariablesEditor.defaultOverrides[aValue]

            if (newValue == null) {
                staticVariableData[rowIndex] = Pair(aValue, aValue.default)
            } else {
                staticVariableData[rowIndex] = Pair(aValue, newValue)
                dataSource[aValue.name] = newValue
            }

            fireTableDataChanged()
        } else if (columnIndex == 1) {
            aValue as String
            val def = getValueAt(rowIndex, 0) as MSSQLStaticDefinition

            staticVariableData[rowIndex] = Pair(def, aValue)
            if (aValue == def.default && def !in MSSQLEnvVariablesEditor.defaultOverrides.keys) {
                dataSource.remove(def.name)
            } else {
                dataSource[def.name] = aValue
            }

            fireTableDataChanged()
        }
    }

    operator fun <T> get(rowIndex: Int, column: Column<Pair<MSSQLStaticDefinition, String>, T>): T {
        return column.getValue(staticVariableData[rowIndex])
    }

    override val columns = MSSQLEnvVariableColumns

    companion object MSSQLEnvVariableColumns : ColumnList<Pair<MSSQLStaticDefinition, String>>() {
        val Key by column(
            value = Pair<MSSQLStaticDefinition, String>::first,
            column = {
                cellEditor = MSSQLEnvironmentVariableTableCellEditor()
                cellRenderer = object : DefaultTableCellRenderer() {
                    override fun getTableCellRendererComponent(
                        table: JTable?,
                        value: Any?,
                        isSelected: Boolean,
                        hasFocus: Boolean,
                        row: Int,
                        column: Int,
                    ): Component {
                        @Suppress("unchecked_cast")
                        table as ReifiedJXTable<MSSQLEnvironmentVariableTableModel>
                        value as MSSQLStaticDefinition

                        val modelRow = table.convertRowIndexToModel(row)

                        toolTipText = if (!table.model.meetsMinimumVersion(modelRow)) {
                            """⚠ Variable will have no effect. ⚠
                                Minimum Version: ${value.minimumVersion}
                                Current Version: ${table.model.version}
                            """.trimIndent()
                        } else {
                            null
                        }
                        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                    }
                }
            },
        )
        val Value by column(
            value = Pair<MSSQLStaticDefinition, String>::second,
            column = {
                cellEditor = MSSQLEnvVariableOptionCellEditor()
            },
        )
    }

    private class MSSQLEnvironmentVariableTableCellEditor : AbstractCellEditor(), TableCellEditor {
        private lateinit var tableRef: ReifiedJXTable<MSSQLEnvironmentVariableTableModel>
        private val comboBox = JComboBox<MSSQLStaticDefinition>().apply {
            configureCellRenderer { _, value, _, _, _ ->
                text = (value as MSSQLStaticDefinition).name
                if (::tableRef.isInitialized) {
                    val minVersion = IgnitionVersionComparator.compare(value.minimumVersion, tableRef.model.version) <= 0
                    background = if (!minVersion) {
                        UIManager.getColor("Actions.Red")
                    } else {
                        null
                    }
                    foreground = if (!minVersion) {
                        Color.WHITE
                    } else {
                        null
                    }
                }
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

        override fun getCellEditorValue(): MSSQLStaticDefinition {
            return comboBox.selectedItem as MSSQLStaticDefinition
        }

        override fun getTableCellEditorComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            row: Int,
            column: Int,
        ): Component {
            @Suppress("unchecked_cast")
            tableRef = table as ReifiedJXTable<MSSQLEnvironmentVariableTableModel>

            val unusedOptions = table.model.getUnusedOptions(forRow = row)

            comboBox.model = DefaultComboBoxModel(unusedOptions.toTypedArray())
            comboBox.selectedItem = value ?: unusedOptions.first()

            return comboBox
        }
    }

    private class MSSQLEnvVariableOptionCellEditor : AbstractCellEditor(), TableCellEditor {
        private val comboBox = JComboBox<String>()
        private val textField = JTextField()

        init {
            comboBox.addItemListener {
                super.fireEditingStopped()
            }
        }

        override fun getCellEditorValue(): Any? {
            return if (comboBox.selectedItem == null) {
                textField.text
            } else {
                comboBox.selectedItem
            }
        }

        override fun isCellEditable(e: EventObject?): Boolean {
            return e is MouseEvent && e.clickCount == 2
        }

        override fun getTableCellEditorComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            row: Int,
            column: Int,
        ): Component {
            @Suppress("unchecked_cast")
            table as ReifiedJXTable<MSSQLEnvironmentVariableTableModel>

            val envVar = table.model[row, table.model.columns.Key]

            if (envVar.options != null) {
                textField.text = null
                comboBox.model = DefaultComboBoxModel(envVar.options.toTypedArray())
                comboBox.selectedItem = value ?: envVar.default
                return comboBox
            } else {
                comboBox.selectedItem = null
                textField.text = value as? String ?: envVar.default
                return textField
            }
        }
    }
}
