package io.github.inductiveautomation.kindling.docker.services.ignition

import io.github.inductiveautomation.kindling.docker.Canvas
import io.github.inductiveautomation.kindling.utils.add
import io.github.inductiveautomation.kindling.utils.getAll
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.event.EventListenerList

class ConnectionObserver(private val canvas: Canvas) {
    var inProgressConnection: IgnitionNodeConnector? = null
        set(value) {
            field = value
            fireConnectionProgressChange()
        }

    private val listenerList = EventListenerList()

    init {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher { e ->
            if (e.keyCode == KeyEvent.VK_ESCAPE) {
                inProgressConnection?.let {
                    canvas.remove(it)
                    it.from.connections.remove(it.index)
                    inProgressConnection = null
                    canvas.repaint()
                }
                true
            } else {
                false
            }
        }
    }

    fun handleConnectionInit(node: IgnitionServiceNode) {
        if (inProgressConnection == null) {
            val index = node.connections.keys.maxOrNull()?.plus(1) ?: 1
            val connection = IgnitionNodeConnector(node, index, canvas)
            node.connections[index] = connection

            canvas.add(connection, Canvas.CONNECTION_LAYER)
            canvas.setLayer(connection, Canvas.CONNECTION_LAYER)

            inProgressConnection = connection
        } else {
            if (validateConnection(inProgressConnection!!.from, node)) {
                inProgressConnection!!.to = node
                inProgressConnection = null
                SwingUtilities.invokeLater {
                    canvas.repaint()
                }
            } else {
                JOptionPane.showMessageDialog(
                    null,
                    "Please select a different node!",
                    "Invalid Connection",
                    JOptionPane.WARNING_MESSAGE,
                )
            }
        }
    }

    fun addConnectionProgressChangeListener(l: ConnectionProgressChangeListener) = listenerList.add(l)

    private fun fireConnectionProgressChange() {
        listenerList.getAll<ConnectionProgressChangeListener>().forEach {
            it.onConnectionProgressChangeRequest(inProgressConnection != null)
        }
    }

    private fun validateConnection(from: IgnitionServiceNode, to: IgnitionServiceNode): Boolean {
        if (from === to) return false
        if (from.model.hostName.isNullOrEmpty() || to.model.hostName.isNullOrEmpty()) return false

        return to.model.networks.any { (k, _) ->
            k in from.model.networks.keys
        } || (from.model.networks.isEmpty() || to.model.networks.isEmpty())
    }

    fun observeConnection(node: IgnitionServiceNode) {
        node.addConnectionInitListener {
            handleConnectionInit(node)
        }

        addConnectionProgressChangeListener { inProgress: Boolean ->
            node.updateValidConnectionTarget(inProgress)
        }
    }
}
