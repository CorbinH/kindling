package io.github.inductiveautomation.kindling.docker.services.mssql

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.docker.volumes.model.DockerVolume
import io.github.inductiveautomation.kindling.docker.services.mssql.model.MSSQLServiceModel
import io.github.inductiveautomation.kindling.docker.services.AbstractDockerServiceNode
import javax.swing.JButton
import javax.swing.JOptionPane
import javax.swing.JPanel
import net.miginfocom.swing.MigLayout

class MSSQLServiceNode(
    model: MSSQLServiceModel,
    initialVolumeOptions: List<DockerVolume>,
    initialNetworkOptions: List<String>,
) : AbstractDockerServiceNode<MSSQLServiceModel>(model) {
    override val configEditor by lazy {
        MSSQLNodeConfigPanel(this, initialVolumeOptions, initialNetworkOptions)
    }

    override var volumeOptions: List<DockerVolume>
        get() = configEditor.volumeOptions
        set(value) {
            configEditor.volumeOptions = value
        }

    override var networkOptions: List<String>
        get() = configEditor.networkOptions
        set(value) {
            configEditor.networkOptions = value
        }

    private val deleteButton = JButton(FlatSVGIcon("icons/bx-x.svg").derive(12, 12)).apply {
        toolTipText = "Delete"
    }

    override val header = JPanel(MigLayout("fill, ins 0")).apply {
        add(deleteButton, "east")
    }

    init {
        add(header, "north")

        deleteButton.addActionListener {
            val confirm = JOptionPane.showConfirmDialog(
                null,
                "Really delete this node?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
            )

            if (confirm == JOptionPane.YES_OPTION) {
                if (configWindow.isVisible) {
                    configWindow.dispose()
                }
                fireNodeDeletedEvent()
            }
        }

        configureButton.addActionListener { configEditor.resetNames() }

        updateHostNameText()
        updateContainerNameText()
    }
}