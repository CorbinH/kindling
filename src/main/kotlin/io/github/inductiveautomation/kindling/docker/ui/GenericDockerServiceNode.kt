package io.github.inductiveautomation.kindling.docker.ui

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.docker.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.model.DockerServiceModel
import io.github.inductiveautomation.kindling.docker.model.DockerVolume
import javax.swing.JButton
import javax.swing.JPanel
import net.miginfocom.swing.MigLayout

class GenericDockerServiceNode(
    override val model: DockerServiceModel,
    initialVolumeOptions: List<DockerVolume>,
    initialNetworkOptions: List<DockerNetwork>,
) : AbstractDockerServiceNode<DockerServiceModel>() {
    override val configEditor = GenericNodeConfigPanel(this, initialVolumeOptions, initialNetworkOptions)

    override var volumeOptions by configEditor::volumeOptions
    override var networkOptions by configEditor::networkOptions

    private val deleteButton = JButton(FlatSVGIcon("icons/bx-x.svg").derive(12, 12))

    override val header = JPanel(MigLayout("fill, ins 0")).apply {
        add(deleteButton, "east")
    }

    init {
        add(header, "growx, spanx", 0)

        updateHostNameText()
        updateContainerNameText()

        deleteButton.addActionListener {
            fireNodeDeletedEvent()
        }
    }

    companion object {
        val SERVICE_NAME_REGEX = """[a-zA-Z0-9][a-zA-Z0-9_.-]+""".toRegex()
        val IMAGE_NAME_REGEX = """.*/(?<serviceName>.*):.*""".toRegex()
    }
}
