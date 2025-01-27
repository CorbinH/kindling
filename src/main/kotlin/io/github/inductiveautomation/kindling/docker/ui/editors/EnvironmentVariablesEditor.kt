package io.github.inductiveautomation.kindling.docker.ui.editors

import io.github.inductiveautomation.kindling.docker.ui.ConfigSection
import io.github.inductiveautomation.kindling.utils.KMutableListModel
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.event.ListDataListener
import net.miginfocom.swing.MigLayout
import org.jdesktop.swingx.JXTextArea

class EnvironmentVariablesEditor(
    initialEnvVariables: MutableList<String>,
) : ConfigSection("Environment Variables") {
    private val envVariablesList = object : JList<String>(KMutableListModel(initialEnvVariables)) {
        override fun getModel(): KMutableListModel<String> = super.getModel() as KMutableListModel<String>
    }

    private val envSectionHeader = JPanel(MigLayout("fill")).apply {
        val envVariableLabel = JLabel("Add/Remove")
        val envEntry = JXTextArea("Enter 1 or more (space-separated)")
        val addEnvButton = JButton("+").apply {
            addActionListener {
                val toAdd = envEntry.text.split(" ").filter { it.isNotBlank() }

                if (toAdd.isNotEmpty()) {
                    envVariablesList.model.addAll(toAdd)
                    envEntry.text = ""
                }
            }
        }
        val removeEnvButton = JButton("-").apply {
            isEnabled = !envVariablesList.isSelectionEmpty

            envVariablesList.selectionModel.addListSelectionListener {
                isEnabled = !(it.source as ListSelectionModel).isSelectionEmpty
            }

            addActionListener {
                envVariablesList.model.removeAll(envVariablesList.selectedIndices.toList())
            }
        }

        add(envVariableLabel, "west")
        add(envEntry, "grow")
        add(addEnvButton, "east")
        add(removeEnvButton, "east")
    }

    init {
        add(envSectionHeader, "growx, spanx")
        add(envVariablesList, "push, grow")
    }

    fun addListDataListener(l: ListDataListener) = envVariablesList.model.addListDataListener(l)
}
