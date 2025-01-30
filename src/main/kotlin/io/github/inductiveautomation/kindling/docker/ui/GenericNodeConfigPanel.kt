package io.github.inductiveautomation.kindling.docker.ui

import io.github.inductiveautomation.kindling.docker.model.DefaultDockerServiceModel
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
import io.github.inductiveautomation.kindling.utils.TrivialListDataListener
import org.jdesktop.swingx.JXFormattedTextField
import javax.swing.JLabel
import javax.swing.JTextField

class GenericNodeConfigPanel(
    private val nodeModel: DefaultDockerServiceModel,
    volumeOptions: Set<DockerVolume>,
    networkOptions: Set<DockerNetwork>,
) : NodeConfigPanel("fill, ins 4") {
    var volumeOptions: Set<DockerVolume> = volumeOptions
        set(value) {
            field = value
            volumesSection.volumeOptions = value
        }

    var networkOptions: Set<DockerNetwork> = networkOptions
        set(value) {
            field = value
            networksSection.networkOptions = value
        }

    /* General */
    private val imageLabel = JLabel("Image")
    private val imageEntry = JTextField(nodeModel.image).apply {
        inputVerifier = RegexInputVerifier(IMAGE_NAME_REGEX)
        addActionListener {
            if (inputVerifier.verify(this)) {
                nodeModel.image = text
                nodeModel.fireServiceModelChangedEvent()
            }
        }
    }

    private val hostLabel = JLabel("Hostname")
    private val hostEntry = JXFormattedTextField("(default)").apply {
        addActionListener {
            nodeModel.hostName = text
            nodeModel.fireServiceModelChangedEvent()
        }
    }

    private val containerLabel = JLabel("Container Name")
    private val containerEntry = JTextField(nodeModel.containerName).apply {
        inputVerifier = RegexInputVerifier(SERVICE_NAME_REGEX)
        addActionListener {
            if (inputVerifier.verify(this)) {
                nodeModel.containerName = text
                nodeModel.fireServiceModelChangedEvent()
            }
        }
    }

    override val generalSection = configSection("General", "fillx, ins 0, aligny top") {
        add(imageLabel)
        add(imageEntry, "growx, wrap")
        add(hostLabel)
        add(hostEntry, "growx, wrap")
        add(containerLabel)
        add(containerEntry, "growx")
    }

    /* Port Mappings */
    override val portsSection = PortMappingEditor(nodeModel.ports).apply {
        addTableModelListener {
            nodeModel.fireServiceModelChangedEvent()
        }
    }

    /* Environment Variables */
    override val envSection = EnvironmentVariablesEditor(nodeModel.environment).apply {
        addListDataListener(
            TrivialListDataListener {
                nodeModel.fireServiceModelChangedEvent()
            }
        )
    }

    /* Command Line Arguments */
    override val cliSection = CliArgumentsEditor(nodeModel.commands).apply {
        addListDataListener(
            TrivialListDataListener {
                nodeModel.fireServiceModelChangedEvent()
            }
        )
    }

    /* Volumes */
    override val volumesSection = VolumeEditor(nodeModel.volumes, volumeOptions).apply {
        addTableModelListener {
            nodeModel.fireServiceModelChangedEvent()
        }
    }

    /* Networks */
    override val networksSection = NetworkEditor(nodeModel.networks, networkOptions).apply {
        addTableModelListener {
            nodeModel.fireServiceModelChangedEvent()
        }
    }

    init {
        add(generalSection, "grow, sg")
        add(portsSection, "grow, sg")
        add(volumesSection, "grow, wrap, sg")
        add(envSection, "grow, sg")
        add(cliSection, "grow, sg")
        add(networksSection, "grow, sg")
    }
}
