package io.github.inductiveautomation.kindling.docker.ui

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.docker.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.model.DockerVolume
import io.github.inductiveautomation.kindling.docker.model.GatewayServiceModel
import io.github.inductiveautomation.kindling.utils.add
import io.github.inductiveautomation.kindling.utils.getAll
import io.github.inductiveautomation.kindling.utils.tag
import java.util.EventListener
import javax.swing.JButton
import javax.swing.JPanel
import net.miginfocom.swing.MigLayout

class GatewayServiceNode(
    override val model: GatewayServiceModel = GatewayServiceModel(),
    initialVolumeOptions: List<DockerVolume>,
    initialNetworkOptions: List<DockerNetwork>,
    val parent: Canvas,
) : AbstractDockerServiceNode<GatewayServiceModel>() {
    override val configEditor by lazy {
        GatewayNodeConfigPanel(this, initialVolumeOptions, initialNetworkOptions)
    }

    override var volumeOptions by configEditor::volumeOptions
    override var networkOptions by configEditor::networkOptions

    private val deleteButton = JButton(FlatSVGIcon("icons/bx-x.svg").derive(12, 12))

    private val connectButton = JButton(FlatSVGIcon("icons/bx-link.svg").derive(12, 12)).apply {
        toolTipText = "Create GAN Connection"

        addActionListener {
            fireConnectionInit()
//            if (this@GatewayServiceNode.model.hostName == null) {
//                JOptionPane.showMessageDialog(
//                    null,
//                    "Please specify a hostname to create a connection",
//                    "Missing Hostname",
//                    JOptionPane.ERROR_MESSAGE
//                )
//            } else {
//                fireConnectionInit()
//            }
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
            println("delete!")
        }

        hostNameLabel.text = buildString {
            tag("html") {
                tag("b") {
                    append("Hostname: ")
                }
                if (model.hostName.isNullOrEmpty()) {
                    tag("i") {
                        append("(default)")
                    }
                } else {
                    append(model.hostName)
                }
            }
        }

        serviceNameLabel.text = buildString {
            tag("html") {
                tag("b") {
                    append("Name: ")
                }
                append(model.containerName)
            }
        }
    }

    fun addConnectionInitListener(l: GatewayConnectionInitListener) {
        listenerList.add(l)
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
