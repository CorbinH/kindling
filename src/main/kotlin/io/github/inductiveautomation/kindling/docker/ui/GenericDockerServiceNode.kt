package io.github.inductiveautomation.kindling.docker.ui

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.docker.model.DefaultDockerServiceModel
import io.github.inductiveautomation.kindling.docker.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.model.DockerVolume
import io.github.inductiveautomation.kindling.utils.tag
import javax.swing.JButton
import javax.swing.JPanel
import net.miginfocom.swing.MigLayout

class GenericDockerServiceNode(
    override val model: DefaultDockerServiceModel = DefaultDockerServiceModel(
        DefaultDockerServiceModel.DEFAULT_GENERIC_IMAGE
    ),
    initialVolumeOptions: Set<DockerVolume>,
    initialNetworkOptions: Set<DockerNetwork>,
) : AbstractDockerServiceNode<DefaultDockerServiceModel>() {
    override var volumeOptions: Set<DockerVolume> = initialVolumeOptions
        set(value) {
            field = value
            configEditor.volumeOptions = value
        }

    override var networks: Set<DockerNetwork> = initialNetworkOptions
        set(value) {
            field = value
            configEditor.networkOptions = value
        }

    private val deleteButton = JButton(FlatSVGIcon("icons/bx-x.svg").derive(12, 12))

    override val header = JPanel(MigLayout("fill, ins 0")).apply {
        add(deleteButton, "east")
    }

    override val configEditor = GenericNodeConfigPanel(model, volumeOptions, networks)

    init {
        add(header, "growx, spanx", 0)

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

    companion object {
        val SERVICE_NAME_REGEX = """[a-zA-Z0-9][a-zA-Z0-9_.-]+""".toRegex()
        val IMAGE_NAME_REGEX = """.*/(?<serviceName>.*):.*""".toRegex()
    }
}
