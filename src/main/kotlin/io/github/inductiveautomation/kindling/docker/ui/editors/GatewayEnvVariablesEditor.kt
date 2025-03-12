package io.github.inductiveautomation.kindling.docker.ui.editors

import io.github.inductiveautomation.kindling.docker.model.GatewayEnvironmentVariableDefinition
import io.github.inductiveautomation.kindling.docker.model.GatewayEnvironmentVariableDefinition.Companion.getConnectionVariableFromInstance
import io.github.inductiveautomation.kindling.docker.model.GatewayEnvironmentVariableDefinition.Companion.isConnectionVariable
import io.github.inductiveautomation.kindling.docker.model.GatewayEnvironmentVariableDefinition.Companion.toYamlString
import io.github.inductiveautomation.kindling.docker.model.IgnitionVersionComparator
import io.github.inductiveautomation.kindling.docker.model.StaticDefinition
import io.github.inductiveautomation.kindling.docker.ui.ConfigSection
import io.github.inductiveautomation.kindling.utils.ColorHighlighter
import io.github.inductiveautomation.kindling.utils.Column
import io.github.inductiveautomation.kindling.utils.ColumnList
import io.github.inductiveautomation.kindling.utils.NoSelectionModel
import io.github.inductiveautomation.kindling.utils.ReifiedJXTable
import io.github.inductiveautomation.kindling.utils.ReifiedTableModel
import io.github.inductiveautomation.kindling.utils.configureCellRenderer
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
import kotlin.properties.Delegates
import net.miginfocom.swing.MigLayout
import org.jdesktop.swingx.JXTextArea

class GatewayEnvVariablesEditor(
    private val data: MutableMap<String, String>,
    version: String,
) : ConfigSection("Environment Variables", "fill, ins 0, gap 4") {
    /**
     * Divided into 3 sections: Pre-canned variables, variables from connection settings, and custom variables.
     */
    private val gatewaySettingsTable = ReifiedJXTable(GatewayEnvironmentVariableTableModel(data, version)).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
    }
    var version: String by gatewaySettingsTable.model::version
    private val gatewaySettingsLabel = JLabel("Ignition Environment Variables")
    private val addButton = JButton("+").apply {
        addActionListener {
            val currentVars = gatewaySettingsTable.model.staticVariableData.map { it.first }
            val newEntry = StaticDefinition.entries.find {
                it !in currentVars
            }

            if (newEntry != null) {
                gatewaySettingsTable.model.staticVariableData.add(Pair(newEntry, newEntry.default))
                gatewaySettingsTable.model.fireTableDataChanged()
            }
        }

        gatewaySettingsTable.model.addTableModelListener {
            isEnabled = (it.source as GatewayEnvironmentVariableTableModel).getUnusedOptions().isNotEmpty()
        }
    }
    private val removeButton = JButton("-").apply {
        isEnabled = false
        gatewaySettingsTable.selectionModel.addListSelectionListener {
            isEnabled = !(it.source as ListSelectionModel).isSelectionEmpty
        }

        addActionListener {
            val index = gatewaySettingsTable.selectionModel.selectedIndices.first()
            val modelIndex = gatewaySettingsTable.convertRowIndexToModel(index)

            val removed = gatewaySettingsTable.model.staticVariableData.removeAt(modelIndex)
            data.remove(removed.first.name)
            gatewaySettingsTable.model.fireTableDataChanged()
        }
    }

    private val customSettingsLabel = JLabel("Custom Environment Variables")
    private val customVariablesTable = ReifiedJXTable(ReifiedMapTableModel(data)).apply {
        isColumnControlVisible = false
        isSortable = false
        setRowFilter(
            object : RowFilter<ReifiedMapTableModel, Int>() {
                override fun include(entry: Entry<out ReifiedMapTableModel, out Int>?): Boolean {
                    val k = entry?.model?.getValueAt(entry.identifier, 0) as String
                    val connectionName = k.getConnectionVariableFromInstance()
                    return GatewayEnvironmentVariableDefinition.variableDefinitionsByName[k] == null && connectionName == null
                }
            }
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
        add(gatewaySettingsLabel, "growx")
        add(removeButton)
        add(addButton, "wrap")
        add(gatewaySettingsTable, "push, grow, span, sg")
        add(customSettingsLabel, "growx, spanx")
        add(customVariablesHeader, "growx, spanx")
        add(customVariablesTable, "push, grow, span, sg")
        add(connectionSettingsLabel, "growx, spanx")
        add(connectionVariablesList, "push, grow, span, sg")

        updateData()

        gatewaySettingsTable.model.addTableModelListener {
            fireConfigChange()
        }

        customVariablesTable.model.addTableModelListener {
            fireConfigChange()
        }

        gatewaySettingsTable.addHighlighter(
            ColorHighlighter(UIManager.getColor("Actions.Red"), Color.WHITE) { _, adapter ->
                val modelRow = gatewaySettingsTable.convertRowIndexToModel(adapter.row)
                !gatewaySettingsTable.model.meetsMinimumVersion(modelRow)
            }
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
                }
            )
        }
    }
}

class GatewayEnvironmentVariableTableModel(
    private val dataSource: MutableMap<String, String>,
    version: String,
) : AbstractTableModel(), ReifiedTableModel<Pair<StaticDefinition, String>> {
    var version by Delegates.observable(version) { _, _, _ ->
        fireTableDataChanged()
    }

    override fun getRowCount() = staticVariableData.size
    override fun getColumnCount() = size
    override fun getColumnClass(columnIndex: Int) = columns[columnIndex].clazz
    override fun getColumnName(columnIndex: Int) = columns[columnIndex].header

    private val allVariables = StaticDefinition.entries.toHashSet()

    /*
     * The table's actual data. Since maps aren't ordered, we need to copy the data here and keep it
     * in sync with the map data.
     */

    internal val staticVariableData: MutableList<Pair<StaticDefinition, String>> = dataSource.filter {
        GatewayEnvironmentVariableDefinition.variableDefinitionsByName.containsKey(it.key)
    }.map { StaticDefinition.valueOf(it.key) to it.value }.toMutableList()

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return columns[columnIndex] == Value || getUnusedOptions().isNotEmpty()
    }

    fun meetsMinimumVersion(rowIndex: Int): Boolean {
        return IgnitionVersionComparator.compare(
            staticVariableData[rowIndex].first.minimumVersion,
            version,
        ) <= 0
    }

    fun getUnusedOptions(forRow: Int? = null): List<StaticDefinition> {
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
            aValue as StaticDefinition
            val currentValue = getValueAt(rowIndex, columnIndex) as StaticDefinition

            if (currentValue.name in dataSource.keys) {
                dataSource.remove(currentValue.name)
            }
            staticVariableData[rowIndex] = Pair(aValue, aValue.default)
            fireTableDataChanged()
        } else if (columnIndex == 1) {
            aValue as String
            val def = getValueAt(rowIndex, 0) as StaticDefinition

            staticVariableData[rowIndex] = Pair(def, aValue)
            if (aValue == def.default) dataSource.remove(def.name) else dataSource[def.name] = aValue

            fireTableDataChanged()
        }
    }

    operator fun <T> get(rowIndex: Int, column: Column<Pair<StaticDefinition, String>, T>): T {
        return column.getValue(staticVariableData[rowIndex])
    }

    override val columns = GatewayEnvVariableColumns

    companion object GatewayEnvVariableColumns : ColumnList<Pair<StaticDefinition, String>>() {
        val Key by column(
            value = Pair<StaticDefinition, String>::first,
            column = {
                cellEditor = GatewayEnvironmentVariableTableCellEditor()
                cellRenderer = object : DefaultTableCellRenderer() {
                    override fun getTableCellRendererComponent(
                        table: JTable?,
                        value: Any?,
                        isSelected: Boolean,
                        hasFocus: Boolean,
                        row: Int,
                        column: Int
                    ): Component {
                        @Suppress("unchecked_cast")
                        table as ReifiedJXTable<GatewayEnvironmentVariableTableModel>
                        value as StaticDefinition

                        val modelRow = table.convertRowIndexToModel(row)
                        if (!table.model.meetsMinimumVersion(modelRow)) {
                            toolTipText = """
                                ⚠ Variable will have no effect. ⚠
                                Minimum Version: ${value.minimumVersion}
                                Current Version: ${table.model.version}
                            """.trimIndent()
                        } else {
                            toolTipText = null
                        }
                        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                    }
                }
            },
        )
        val Value by column(
            value = Pair<StaticDefinition, String>::second,
            column = {
                cellEditor = GatewayEnvVariableOptionCellEditor()
            },
        )
    }

    private class GatewayEnvironmentVariableTableCellEditor : AbstractCellEditor(), TableCellEditor {
        private lateinit var tableRef: ReifiedJXTable<GatewayEnvironmentVariableTableModel>
        private val comboBox = JComboBox<StaticDefinition>().apply {
            configureCellRenderer { _, value, _, _, _ ->
                text = (value as StaticDefinition).name
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

        override fun getCellEditorValue(): StaticDefinition {
            return comboBox.selectedItem as StaticDefinition
        }

        override fun getTableCellEditorComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            row: Int,
            column: Int,
        ): Component {
            @Suppress("unchecked_cast")
            tableRef = table as ReifiedJXTable<GatewayEnvironmentVariableTableModel>

            val unusedOptions = table.model.getUnusedOptions(forRow = row)

            comboBox.model = DefaultComboBoxModel(unusedOptions.toTypedArray())
            comboBox.selectedItem = value ?: unusedOptions.first()

            return comboBox
        }
    }

    private class GatewayEnvVariableOptionCellEditor : AbstractCellEditor(), TableCellEditor {
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
            column: Int
        ): Component {
            @Suppress("unchecked_cast")
            table as ReifiedJXTable<GatewayEnvironmentVariableTableModel>

            val envVar = table.model[row, table.model.columns.Key]

            if (envVar.options != null) {
                textField.text = null
                comboBox.model = DefaultComboBoxModel(envVar.options!!.toTypedArray())
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
