package io.github.inductiveautomation.kindling.docker.ui

import io.github.inductiveautomation.kindling.docker.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.model.DockerVolume
import io.github.inductiveautomation.kindling.docker.ui.GenericDockerServiceNode.Companion.IMAGE_NAME_REGEX
import io.github.inductiveautomation.kindling.docker.ui.GenericDockerServiceNode.Companion.SERVICE_NAME_REGEX
import io.github.inductiveautomation.kindling.docker.ui.editors.CliArgumentsEditor
import io.github.inductiveautomation.kindling.docker.ui.editors.EnvironmentVariablesEditor
import io.github.inductiveautomation.kindling.docker.ui.editors.NetworkEditor
import io.github.inductiveautomation.kindling.docker.ui.editors.PortMappingEditor
import io.github.inductiveautomation.kindling.docker.ui.editors.VolumeEditor
import io.github.inductiveautomation.kindling.utils.RegexInputVerifier
import javax.swing.JLabel
import javax.swing.JTextField
import org.jdesktop.swingx.JXFormattedTextField

class GenericNodeConfigPanel(
    override val node: GenericDockerServiceNode,
    volumeOptions: List<DockerVolume>,
    networkOptions: List<DockerNetwork>,
) : NodeConfigPanel("fill, ins 4") {
    /* General */
    private val imageLabel = JLabel("Image")
    private val imageEntry = JTextField(node.model.image).apply {
        inputVerifier = RegexInputVerifier(IMAGE_NAME_REGEX)
        addActionListener {
            if (inputVerifier.verify(this)) {
                node.model.image = text
                node.fireServiceModelChangedEvent()
            }
        }
    }

    private val hostLabel = JLabel("Hostname")
    private val hostEntry = JXFormattedTextField("(default)").apply {
        addActionListener {
            node.model.hostName = text
            node.fireServiceModelChangedEvent()
        }
    }

    private val containerLabel = JLabel("Container Name")
    private val containerEntry = JTextField(node.model.containerName).apply {
        inputVerifier = RegexInputVerifier(SERVICE_NAME_REGEX)
        addActionListener {
            if (inputVerifier.verify(this)) {
                node.model.containerName = text
                node.fireServiceModelChangedEvent()
            }
        }
    }

    override val generalSection = object : ConfigSection("General", "fillx, ins 0, aligny top") {
        init {
            add(imageLabel)
            add(imageEntry, "growx, wrap")
            add(hostLabel)
            add(hostEntry, "growx, wrap")
            add(containerLabel)
            add(containerEntry, "growx")
        }
    }

    override val portsSection = PortMappingEditor(node.model.ports).bind()
    override val envSection = EnvironmentVariablesEditor(node.model.environment).bind()
    override val cliSection = CliArgumentsEditor(node.model.commands).bind()
    override val volumesSection = VolumeEditor(node.model.volumes, volumeOptions).bind()
    override val networksSection = NetworkEditor(node.model.networks, networkOptions).bind()

    var volumeOptions by volumesSection::volumeOptions
    var networkOptions by networksSection::networkOptions

    init {
        add(generalSection, "grow, sg")
        add(envSection, "grow, sg")
        add(volumesSection, "grow, wrap, sg")
        add(cliSection, "grow, sg")
        add(portsSection, "grow, sg")
        add(networksSection, "grow, sg")
    }
}
