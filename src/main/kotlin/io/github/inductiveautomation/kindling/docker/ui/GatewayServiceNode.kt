package io.github.inductiveautomation.kindling.docker.ui

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.docker.model.GatewayServiceModel
import io.github.inductiveautomation.kindling.utils.tag
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import net.miginfocom.swing.MigLayout

class GatewayServiceNode(
    override val model: GatewayServiceModel = GatewayServiceModel(GatewayServiceModel.DEFAULT_IMAGE),
) : AbstractDockerServiceNode<GatewayServiceModel>() {

    override val configEditor: JComponent = JPanel()

    private val deleteButton = JButton(FlatSVGIcon("icons/bx-x.svg").derive(12, 12))

    private val connectButton = JButton(FlatSVGIcon("icons/bx-link.svg").derive(12, 12)).apply {
        toolTipText = "Create Outgoing GAN Connection"
    }

    override val header = JPanel(MigLayout("fill, ins 0")).apply {
        add(connectButton, "west")
        add(deleteButton, "east")
    }

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
}

enum class GatewayServiceFlavor {
    KCOLLINS,
    INDUCTIVEAUTOMATION,
    ;

    val serialName: String = name.lowercase()
}

/*
    init {
        ports.putAll(
            listOf(
                9088.toUShort() to 8088.toUShort(),
                9043.toUShort() to 8043.toUShort(),
            )
        )

        environment.addAll(
            listOf(
                "ACCEPT_IGNITION_EULA=Y",
                "GATEWAY_ADMIN_USERNAME=admin",
                "GATEWAY_ADMIN_PASSWORD_FILE=/run/secrets/gateway-admin-password",
                "IGNITION_EDITION=standard",
                "TZ=America/Chicago",
            )
        )

        commands.addAll(
            listOf(
                "-n docker-test",
                "-m 1024",
                "wrapper.java.initmemory=512",
                "-Dignition.allowunsignedmodules=true",
            ),
        )
    }
*/
