package io.github.inductiveautomation.kindling.docker.ui

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.docker.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.model.DockerVolume
import io.github.inductiveautomation.kindling.docker.model.GatewayServiceModel
import io.github.inductiveautomation.kindling.utils.add
import io.github.inductiveautomation.kindling.utils.getAll
import java.util.EventListener
import javax.swing.JButton
import javax.swing.JOptionPane
import javax.swing.JPanel
import net.miginfocom.swing.MigLayout

class GatewayServiceNode(
    override val model: GatewayServiceModel,
    initialVolumeOptions: List<DockerVolume>,
    initialNetworkOptions: List<DockerNetwork>,
) : AbstractDockerServiceNode<GatewayServiceModel>() {
    override val configEditor by lazy {
        GatewayNodeConfigPanel(this, initialVolumeOptions, initialNetworkOptions)
    }

    override var volumeOptions by configEditor::volumeOptions
    override var networkOptions by configEditor::networkOptions

    private val deleteButton = JButton(FlatSVGIcon("icons/bx-x.svg").derive(12, 12)).apply {
        toolTipText = "Delete"
    }

    private val connectButton = JButton(FlatSVGIcon("icons/bx-link.svg").derive(12, 12)).apply {
        toolTipText = "Create GAN Connection"

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
                fireNodeDeletedEvent()
            }
        }

        updateHostNameText()
        updateContainerNameText()
    }

    fun addConnectionInitListener(l: GatewayConnectionInitListener) = listenerList.add(l)

    private fun fireConnectionInit() {
        listenerList.getAll<GatewayConnectionInitListener>().forEach {
            it.onConnectionInitRequest()
        }
    }
}

fun interface GatewayConnectionInitListener : EventListener {
    fun onConnectionInitRequest()
}
