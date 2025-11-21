package io.github.inductiveautomation.kindling.docker.services.generic

import io.github.inductiveautomation.kindling.docker.services.ConfigSection
import io.github.inductiveautomation.kindling.utils.FlatScrollPane
import io.github.inductiveautomation.kindling.utils.ReifiedJXTable
import io.github.inductiveautomation.kindling.utils.ReifiedMapTableModel
import io.github.inductiveautomation.kindling.utils.StringPairColumns
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import net.miginfocom.swing.MigLayout
import org.jdesktop.swingx.JXTextArea

class EnvironmentVariablesEditor(
    private val envVariables: MutableMap<String, String>,
) : ConfigSection("Environment Variables") {
    private val envVariablesTable = ReifiedJXTable(ReifiedMapTableModel(envVariables, StringPairColumns)).apply {
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