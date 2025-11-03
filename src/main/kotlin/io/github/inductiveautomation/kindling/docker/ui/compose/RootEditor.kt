package io.github.inductiveautomation.kindling.docker.ui.compose

import io.github.inductiveautomation.kindling.docker.model.DockerServiceModel
import io.github.inductiveautomation.kindling.docker.model.compose.DependsOn
import io.github.inductiveautomation.kindling.docker.model.compose.Restart
import io.github.inductiveautomation.kindling.utils.ColumnList
import io.github.inductiveautomation.kindling.utils.FlatScrollPane
import io.github.inductiveautomation.kindling.utils.ReifiedJXTable
import io.github.inductiveautomation.kindling.utils.ReifiedMapTableModel
import net.miginfocom.swing.MigLayout
import java.util.EventListener
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.ListSelectionModel

class RootEditor(data: DockerServiceModel) : ComposeObjectEditor<DockerServiceModel>("root", data) {
    val deploy by composeObject(DeployEditor(data.deploy))
    val build by composeObject(BuildEditor(data.build))
    val attach by checkbox(value = data.attach) {
        data.attach = it
    }
    val entrypoint by list(value = data.entrypoint)
    val restart by combo(options = Restart.entries, initialValue = data.restart) {
        data.restart = it
    }
    val envFile by list("Env Files", value = data.envFile)
    val labels by map(value = data.labels)
    val dependencies by composeValue {
        val nameLabel = JLabel("Name")
        val nameEntry = JTextField()

        val restartCheckbox = JCheckBox("Restart")

        val conditionLabel = JLabel("Condition")
        val conditionCombo = JComboBox(DependsOn.Condition.entries.toTypedArray())

        val requiredCheckbox = JCheckBox("Required").apply {
            isSelected = true
        }

        val table = ReifiedJXTable(ReifiedMapTableModel(data.dependsOn, DependsOnColumns)).apply {
            isColumnControlVisible = false
        }

        val addButton = JButton("+").apply {
            addActionListener {
                if (nameEntry.text.isNotBlank() && conditionCombo.selectedIndex >= 0) {
                    data.dependsOn[nameEntry.text] = DependsOn(
                        restart = restartCheckbox.isSelected,
                        condition = conditionCombo.selectedItem as DependsOn.Condition,
                        required = requiredCheckbox.isSelected,
                    )
                    table.model.fireTableDataChanged()
                }
            }
        }

        val removeButton = JButton("-").apply {
            isEnabled = false
            table.selectionModel.addListSelectionListener {
                isEnabled = !(it.source as ListSelectionModel).isSelectionEmpty
            }

            addActionListener {
                val key = table.model.getValueAt(table.selectedRow, 0) as String?
                if (key != null) {
                    data.dependsOn.remove(key)
                    table.model.fireTableDataChanged()
                }
            }
        }

        table.model.addTableModelListener {
            fireValueChanged()
        }

        JPanel(MigLayout("fill, ins 0")).apply {
            add(nameLabel, "split")
            add(nameEntry, "growx")
            add(restartCheckbox, "growx")
            add(conditionLabel, "split")
            add(conditionCombo, "growx")
            add(requiredCheckbox, "growx, wrap")

            add(removeButton)
            add(addButton, "wrap")
            add(FlatScrollPane(table), "push, grow, span")
        }
    }

    private val listeners: MutableList<ValueChangeListener> = mutableListOf()

    fun interface ValueChangeListener : EventListener {
        fun valueChange()
    }

    fun addValueChangeListener(l: ValueChangeListener) {
        listeners.add(l)
    }

    fun fireValueChanged() {
        listeners.forEach {
            it.valueChange()
        }
    }
}

object DependsOnColumns : ColumnList<Map.Entry<String, DependsOn>>() {
    val Name by column {
        it.key
    }
    val Condition by column {
        it.value.condition
    }
    val Restart by column {
        it.value.restart
    }
    val Required by column {
        it.value.required
    }
}
