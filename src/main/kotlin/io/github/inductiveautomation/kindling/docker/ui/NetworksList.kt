package io.github.inductiveautomation.kindling.docker.ui

import io.github.inductiveautomation.kindling.docker.model.DockerNetwork
import io.github.inductiveautomation.kindling.utils.FlatScrollPane
import io.github.inductiveautomation.kindling.utils.listCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import net.miginfocom.swing.MigLayout
import org.jdesktop.swingx.JXTextArea

class NetworksList(networks: List<DockerNetwork>) : JPanel(MigLayout("fill, ins 0")) {
    val networksList = object : JList<DockerNetwork>(DefaultListModel()) {
        override fun getModel(): DefaultListModel<DockerNetwork> = super.getModel() as DefaultListModel<DockerNetwork>
        init {
            cellRenderer = listCellRenderer<DockerNetwork> { _, value, _, _, _ ->
                text = value.name
            }

            model.addAll(networks)
        }
    }

    private val networkNameEntry = JXTextArea("Network name")

    private val addButton = JButton("+")
    private val removeButton = JButton("-").apply {
        isEnabled = false
    }

    init {
        add(networkNameEntry, "pushx, growx")
        add(removeButton)
        add(addButton, "wrap")
        add(FlatScrollPane(networksList), "push, grow, span")

        addButton.addActionListener {
            val name = networkNameEntry.text?.trim() ?: ""
            if (name.isNotBlank()) {
                val model = networksList.model as DefaultListModel<DockerNetwork>

                for (i in 0..<model.size) {
                    if (model[i].name == name) return@addActionListener
                }

                model.addElement(DockerNetwork(name))
            }
        }

        removeButton.addActionListener {
            if (!networksList.selectionModel.isSelectionEmpty) {
                val toRemove = networksList.selectedValuesList
                toRemove.forEach {
                    (networksList.model as DefaultListModel<DockerNetwork>).removeElement(it)
                }
            }
        }

        networksList.selectionModel.addListSelectionListener {
            removeButton.isEnabled = !(it.source as ListSelectionModel).isSelectionEmpty
        }
    }
}