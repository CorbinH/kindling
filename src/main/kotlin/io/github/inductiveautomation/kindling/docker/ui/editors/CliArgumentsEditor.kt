package io.github.inductiveautomation.kindling.docker.ui.editors

import io.github.inductiveautomation.kindling.docker.model.CLI_REGEX
import io.github.inductiveautomation.kindling.docker.model.CliArgument
import io.github.inductiveautomation.kindling.docker.ui.ConfigSection
import io.github.inductiveautomation.kindling.utils.KMutableListModel
import io.github.inductiveautomation.kindling.utils.RegexInputVerifier
import io.github.inductiveautomation.kindling.utils.TrivialListDataListener
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import net.miginfocom.swing.MigLayout
import org.jdesktop.swingx.JXTextField

class CliArgumentsEditor(
    initialCliArguments: MutableList<CliArgument>,
) : ConfigSection("Command Line Arguments") {
    private val cliArgumentsList = object : JList<CliArgument>(KMutableListModel(initialCliArguments)) {
        override fun getModel(): KMutableListModel<CliArgument> = super.getModel() as KMutableListModel<CliArgument>
    }

    private val cliSectionHeader = JPanel(MigLayout("fill")).apply {
        val cliLabel = JLabel("Add/Remove")
        val cliEntry = JXTextField("Enter argument(s) to add and press (+)").apply {
            inputVerifier = RegexInputVerifier(CLI_REGEX, true)
        }

        val addCliButton = JButton("+").apply {
            addActionListener {
                if (cliEntry.inputVerifier.verify(cliEntry)) {
                    cliArgumentsList.model.addAll(
                        CLI_REGEX.findAll(cliEntry.text).map { it.value }.toList()
                    )
                    cliEntry.text = ""
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
}