package io.github.inductiveautomation.kindling.docker.ui.editors

import io.github.inductiveautomation.kindling.docker.model.CliArgument
import io.github.inductiveautomation.kindling.docker.model.GatewayCommandLineArgument
import io.github.inductiveautomation.kindling.docker.ui.ConfigSection
import io.github.inductiveautomation.kindling.utils.KMutableListModel
import io.github.inductiveautomation.kindling.utils.TrivialListDataListener
import io.github.inductiveautomation.kindling.utils.configureCellRenderer
import javax.swing.DefaultComboBoxModel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import net.miginfocom.swing.MigLayout

class GatewayCliArgEditor(
    private val args: MutableList<CliArgument>,
) : ConfigSection("Command Line Arguments") {
    private val cliArgumentsList = object : JList<CliArgument>(KMutableListModel(args)) {
        override fun getModel(): KMutableListModel<CliArgument> = super.getModel() as KMutableListModel<CliArgument>
    }

    private val cliSectionHeader = JPanel(MigLayout("fill")).apply {
        val cliLabel = JLabel("Add/Remove")
        val cliEntry = JComboBox<String>().apply {
            isEditable = true

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
                val toAdd = cliEntry.editor.item as String
                if (toAdd !in args) {
                    cliArgumentsList.model.add(toAdd)
                    cliEntry.selectedIndex = -1
                }
            }
        }

        val removeCliButton = JButton("-").apply {
            isEnabled = false

            cliArgumentsList.selectionModel.addListSelectionListener {
                isEnabled = !(it.source as ListSelectionModel).isSelectionEmpty
            }

            addActionListener {
                cliArgumentsList.model.removeAll(cliArgumentsList.selectedIndices.toList())
            }
        }

        add(cliLabel, "west")
        add(cliEntry, "grow")
        add(addCliButton, "east")
        add(removeCliButton, "east")
    }

    init {
        add(cliSectionHeader, "growx, spanx")
        add(cliArgumentsList, "push, grow")

        cliArgumentsList.model.addListDataListener(
            TrivialListDataListener {
                fireConfigChange()
            }
        )
    }

    companion object {
        private val flags = GatewayCommandLineArgument.entries.associateBy { it.flag }
    }
}
