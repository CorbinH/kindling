package io.github.inductiveautomation.kindling.docker.ui

import io.github.inductiveautomation.kindling.docker.model.DockerVolume
import io.github.inductiveautomation.kindling.utils.listCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import net.miginfocom.swing.MigLayout
import org.jdesktop.swingx.JXTextArea

class VolumesList(volumes: List<DockerVolume>) : JPanel(MigLayout("fill, ins 0")) {
    val volumesList = object : JList<DockerVolume>(DefaultListModel()) {
        override fun getModel(): DefaultListModel<DockerVolume> = super.getModel() as DefaultListModel<DockerVolume>
        init {
            cellRenderer = listCellRenderer<DockerVolume> { _, value, _, _, _ ->
                text = value.name
            }

            model.addAll(volumes)
        }
    }

    private val volumeNameEntry = JXTextArea("Volume name")

    private val addButton = JButton("+")
    private val removeButton = JButton("-").apply {
        isEnabled = false
    }

    init {
        add(volumeNameEntry, "pushx, growx")
        add(removeButton)
        add(addButton, "wrap")
        add(volumesList, "push, grow, span")

        addButton.addActionListener {
            if (!volumeNameEntry.text.isNullOrEmpty()) {
                (volumesList.model as DefaultListModel<DockerVolume>).addElement(DockerVolume(volumeNameEntry.text))
            }
        }

        removeButton.addActionListener {
            if (!volumesList.selectionModel.isSelectionEmpty) {
                val toRemove = volumesList.selectedValuesList
                toRemove.forEach {
                    (volumesList.model as DefaultListModel<DockerVolume>).removeElement(it)
                }
            }
        }

        volumesList.selectionModel.addListSelectionListener {
            removeButton.isEnabled = !(it.source as ListSelectionModel).isSelectionEmpty
        }
    }
}