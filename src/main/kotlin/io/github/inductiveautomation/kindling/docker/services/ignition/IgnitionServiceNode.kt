package io.github.inductiveautomation.kindling.docker.services.ignition

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.docker.services.AbstractDockerServiceNode
import io.github.inductiveautomation.kindling.docker.services.ignition.model.IgnitionServiceModel
import io.github.inductiveautomation.kindling.docker.services.ignition.model.IgnitionVersionComparator
import io.github.inductiveautomation.kindling.docker.services.ignition.model.gatewayHostPort
import io.github.inductiveautomation.kindling.docker.volumes.model.DockerVolume
import io.github.inductiveautomation.kindling.utils.add
import io.github.inductiveautomation.kindling.utils.getAll
import net.miginfocom.swing.MigLayout
import java.awt.Desktop
import java.net.URI
import java.util.EventListener
import javax.swing.JButton
import javax.swing.JOptionPane
import javax.swing.JPanel

class IgnitionServiceNode(
    model: IgnitionServiceModel,
    initialVolumeOptions: List<DockerVolume>,
    initialNetworkOptions: List<String>,
) : AbstractDockerServiceNode<IgnitionServiceModel>(model) {
    override val configEditor by lazy {
        IgnitionNodeConfigPanel(this, initialVolumeOptions, initialNetworkOptions)
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

    private val meetsMinVersion: Boolean
        get() = IgnitionVersionComparator.compare("8.1.10", model.version) <= 0

    private val deleteButton = JButton(FlatSVGIcon("icons/bx-x.svg").derive(12, 12)).apply {
        toolTipText = "Delete"
    }

    private val connectButton = JButton(FlatSVGIcon("icons/bx-link.svg").derive(12, 12)).apply {
        toolTipText = if (meetsMinVersion) null else "GAN connections only available for 8.1.10+"

        addActionListener {
            fireConnectionInit()
        }
    }

    private val openGatewayButton = JButton(FlatSVGIcon("icons/bx-link-external.svg").derive(12, 12)).apply {
        toolTipText = "Open gateway in browser"
        isVisible = false

        addActionListener {
            openGateway()
        }
    }

    override val header = JPanel(MigLayout("fill, ins 0")).apply {
        add(connectButton, "west")
        add(openGatewayButton, "west")
        add(deleteButton, "east")
    }

    // Whether the compose stack is currently running; pushed down from the control bar via the panel.
    private var stackRunning = false

    val connections: MutableMap<Int, IgnitionNodeConnector> = mutableMapOf()

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
        model.addServiceModelChangeListener {
            connectButton.isEnabled = meetsMinVersion
            // The gwbk command, GATEWAY_HTTP_PORT override, or port binding may have changed.
            updateGatewayButtonVisibility()
        }
    }

    /** Called by the panel whenever the compose stack's run-state changes. */
    fun onStackRunningChanged(running: Boolean) {
        stackRunning = running
        updateGatewayButtonVisibility()
    }

    private fun updateGatewayButtonVisibility() {
        val visible = stackRunning && model.gatewayHostPort != null
        if (visible != openGatewayButton.isVisible) {
            openGatewayButton.isVisible = visible
            // Toggling a header button changes the node's preferred size; relayout.
            revalidate()
            repaint()
        }
    }

    private fun openGateway() {
        val port = model.gatewayHostPort ?: return
        runCatching { Desktop.getDesktop().browse(URI("http://localhost:$port")) }
    }

    fun addConnectionInitListener(l: GatewayConnectionInitListener) = listenerList.add(l)

    fun updateValidConnectionTarget(inProgress: Boolean) {
        if (inProgress) {
            connectButton.isEnabled = true
        } else {
            connectButton.isEnabled = meetsMinVersion
        }
    }

    private fun fireConnectionInit() {
        listenerList.getAll<GatewayConnectionInitListener>().forEach {
            it.onConnectionInitRequest()
        }
    }
}

fun interface GatewayConnectionInitListener : EventListener {
    fun onConnectionInitRequest()
}

fun interface ConnectionProgressChangeListener : EventListener {
    fun onConnectionProgressChangeRequest(inProgress: Boolean)
}
