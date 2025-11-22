package io.github.inductiveautomation.kindling.docker.networks

import io.github.inductiveautomation.kindling.docker.compose.ComposeEditor
import io.github.inductiveautomation.kindling.docker.compose.NetworkEditor
import io.github.inductiveautomation.kindling.docker.networks.model.DockerNetwork
import io.github.inductiveautomation.kindling.utils.Action
import io.github.inductiveautomation.kindling.utils.ColumnList
import io.github.inductiveautomation.kindling.utils.FlatScrollPane
import io.github.inductiveautomation.kindling.utils.HorizontalSplitPane
import io.github.inductiveautomation.kindling.utils.ReifiedJXTable
import io.github.inductiveautomation.kindling.utils.ReifiedMapTableModel
import io.github.inductiveautomation.kindling.utils.jFrame
import io.github.inductiveautomation.kindling.utils.treeCellRenderer
import java.awt.Component
import java.awt.Font
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.EventObject
import javax.swing.AbstractCellEditor
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.JTree
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellEditor
import javax.swing.tree.DefaultTreeModel
import net.miginfocom.swing.MigLayout
import org.jdesktop.swingx.JXTextArea

class NetworksTab(
    private val networks: MutableMap<String, DockerNetwork>,
    onUpdate: () -> Unit,
) : JPanel(MigLayout("fill, ins 4")) {
    private val titleLabel = JLabel("Networks").apply {
        font = font.deriveFont(Font.BOLD, 16F)
    }
    private val networkKeyEntry = JXTextArea("Network Key")
    private val addButton = JButton(
        Action("+", "Add network") {
            if (networkKeyEntry.text.isNotBlank()) {
                networks[networkKeyEntry.text] = DockerNetwork()
                table.model.fireTableDataChanged()
            }
        }
    )
    private val removeButton = JButton(
        Action("-", "Delete selected network") {
            val index = table.selectedRow
            if (index == -1) return@Action

            val modelIndex = table.convertRowIndexToModel(index)
            val key = table.model.getValueAt(modelIndex, 0) as String

            if (networks.remove(key) != null) {
                table.model.fireTableDataChanged()
            }
        }
    )

    val table = ReifiedJXTable(ReifiedMapTableModel(networks, NetworksColumns)).apply {
        isColumnControlVisible = false
        selectionMode = ListSelectionModel.SINGLE_SELECTION
    }

    init {
        add(titleLabel, "wrap")
        add(networkKeyEntry, "pushx, growx")
        add(removeButton)
        add(addButton, "wrap")
        add(FlatScrollPane(table), "push, grow, span")

        table.model.addTableModelListener {
            onUpdate()
        }
    }
}

class NetworkEditorPanel(
    initialNetwork: DockerNetwork,
    private val onUpdate: () -> Unit,
) : JPanel(MigLayout("fill")) {
    var network = initialNetwork
        set(value) {
            field = value
            editorArea.removeAll()

            val newEditor = NetworkEditor(value).apply {
                addValueChangeListener {
                    onUpdate()
                }
            }
            tree.model = DefaultTreeModel(newEditor)
        }

    private val tree = JTree().apply {
        model = DefaultTreeModel(
            NetworkEditor(initialNetwork).apply {
                addValueChangeListener { onUpdate() }
            },
        )

        showsRootHandles = false

        cellRenderer = treeCellRenderer { _, value, _, _, _, _, _ ->
            text = (value as? ComposeEditor)?.name
            this@treeCellRenderer
        }

        addTreeSelectionListener {
            val comp = it.path.lastPathComponent as ComposeEditor
            editorArea.removeAll()
            editorArea.add(comp.component, "push, grow, span")
            SwingUtilities.invokeLater {
                editorArea.revalidate()
                editorArea.repaint()
            }
        }
    }

    private val editorArea = JPanel(MigLayout("fill, ins 4"))

    init {
        add(
            HorizontalSplitPane(
                FlatScrollPane(tree),
                FlatScrollPane(editorArea),
                0.2,
            ),
            "push, grow, span",
        )
    }
}

@Suppress("unused")
object NetworksColumns : ColumnList<Map.Entry<String, DockerNetwork>>() {
    val yamlKey by column("YAML Key") {
        it.key
    }

    val Network by column(
        column = {
            cellRenderer = object : DefaultTableCellRenderer() {
                override fun getTableCellRendererComponent(
                    table: JTable?,
                    value: Any?,
                    isSelected: Boolean,
                    hasFocus: Boolean,
                    row: Int,
                    column: Int
                ): Component {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                    text = "Edit"
                    return this
                }
            }

            cellEditor = DockerNetworkCellEditor()
        },
        value = { it.value }
    )

    class DockerNetworkCellEditor : AbstractCellEditor(), TableCellEditor {
        private lateinit var editorPanel: NetworkEditorPanel

        private val label = JLabel("Editing...")

        private val dialog by lazy {
            jFrame("Edit Network", 800, 600, false) {
                add(editorPanel)
                addWindowListener(
                    object : WindowAdapter() {
                        override fun windowClosed(e: WindowEvent?) {
                            super.windowClosed(e)
                            fireEditingStopped()
                        }
                    }
                )
            }
        }

        override fun getCellEditorValue() = editorPanel.network

        override fun getTableCellEditorComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            row: Int,
            column: Int,
        ): Component {
            if (!::editorPanel.isInitialized) {
                editorPanel = NetworkEditorPanel(value as DockerNetwork) {
                    (table?.model as AbstractTableModel?)?.fireTableDataChanged()
                }
            } else {
                editorPanel.network = value as DockerNetwork
            }

            SwingUtilities.invokeLater {
                dialog.isVisible = true
            }

            return label
        }

        override fun isCellEditable(e: EventObject?): Boolean {
            return (e is MouseEvent && e.clickCount == 2)
        }
    }
}