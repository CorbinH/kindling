package io.github.inductiveautomation.kindling.docker.ui

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.docker.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.model.DockerVolume
import io.github.inductiveautomation.kindling.docker.model.MSSQLServiceModel
import io.github.inductiveautomation.kindling.utils.getAll
import net.miginfocom.swing.MigLayout
import java.util.EventListener
import javax.swing.JButton
import javax.swing.JOptionPane
import javax.swing.JPanel

class MSSQLServiceNode(
    override val model: MSSQLServiceModel,
    initialVolumeOptions: List<DockerVolume>,
    initialNetworkOptions: List<DockerNetwork>,
) : AbstractDockerServiceNode<MSSQLServiceModel>() {
    override val configEditor by lazy {
        MSSQLNodeConfigPanel(this, initialVolumeOptions, initialNetworkOptions)
    }

    override var volumeOptions: List<DockerVolume>
        get() = configEditor.volumeOptions
        set(value) {
            configEditor.volumeOptions = value
        }

    override var networkOptions: List<DockerNetwork>
        get() = configEditor.networkOptions
        set(value) {
            configEditor.networkOptions = value
        }

    private val deleteButton = JButton(FlatSVGIcon("icons/bx-x.svg").derive(12, 12)).apply {
        toolTipText = "Delete"
    }

    private val connectButton = JButton(FlatSVGIcon("icons/bx-link.svg").derive(12, 12)).apply {
        addActionListener {
            fireConnectionInit()
        }
    }

    override val header = JPanel(MigLayout("fill, ins 0")).apply {
        add(connectButton, "west")
        add(deleteButton, "east")
    }

    val connections: MutableMap<Int, GatewayNodeConnector> = mutableMapOf()

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
        addServiceModelChangeListener {
            connectButton.isEnabled = true
        }
    }

    private fun fireConnectionInit() {
        listenerList.getAll<MSSQLConnectionInitListener>().forEach {
            it.onConnectionInitRequest()
        }
    }
}

fun interface MSSQLConnectionInitListener : EventListener {
    fun onConnectionInitRequest()
}
