package io.github.inductiveautomation.kindling.utils

import javax.swing.table.AbstractTableModel

class ReifiedMapTableModel<V>(
    val data: Map<String, V>,
    override val columns: ColumnList<Map.Entry<String, V>>,
) : AbstractTableModel(), ReifiedTableModel<Map.Entry<String, V>> {
    override fun getRowCount() = data.size
    override fun getColumnCount() = columns.size
    override fun getColumnClass(columnIndex: Int) = columns[columnIndex].clazz
    override fun getColumnName(columnIndex: Int) = columns[columnIndex].header
    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        return columns[columnIndex].getValue(data.entries.toList()[rowIndex])
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int) = true
}

object StringPairColumns : ColumnList<Map.Entry<String, String>>() {
    val Key by column { it.key }
    val Value by column { it.value }
}
