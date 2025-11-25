package io.github.inductiveautomation.kindling.docker.services.generic

import io.github.inductiveautomation.kindling.docker.volumes.model.DockerVolume
import io.github.inductiveautomation.kindling.docker.services.ConfigSection
import io.github.inductiveautomation.kindling.docker.services.NodeConfigPanel
import io.github.inductiveautomation.kindling.utils.RegexInputVerifier
import javax.swing.JLabel
import javax.swing.JTextField
import org.jdesktop.swingx.JXFormattedTextField

class GenericNodeConfigPanel(
    override val node: GenericDockerServiceNode,
    volumeOptions: List<DockerVolume>,
    networkOptions: List<String>,
) : NodeConfigPanel() {
    /* General */
    private val imageLabel = JLabel("Image")
    private val imageEntry = JTextField(node.model.image).apply {
        inputVerifier = RegexInputVerifier(GenericDockerServiceNode.IMAGE_NAME_REGEX)
        addActionListener {
            if (inputVerifier.verify(this)) {
                node.model.image = text
                node.model.fireServiceModelChangedEvent()
            }
        }
    }

    private val hostLabel = JLabel("Hostname")
    private val hostEntry = JXFormattedTextField("(default)").apply {
        addActionListener {
            node.model.hostName = text
            node.model.fireServiceModelChangedEvent()
        }
    }

    private val containerLabel = JLabel("Container Name")
    private val containerEntry = JTextField(node.model.containerName).apply {
        inputVerifier = RegexInputVerifier(GenericDockerServiceNode.SERVICE_NAME_REGEX)
        addActionListener {
            if (inputVerifier.verify(this)) {
                node.model.containerName = text
                node.model.fireServiceModelChangedEvent()
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
    override val networksSection = NetworkConnectionEditor(node.model.networks, networkOptions).bind()
    override val genericSection by lazy {
        ComposeEditor(node.model).bind()
    }

    var volumeOptions by volumesSection::volumeOptions
    var networkOptions by networksSection::networkOptions

    init {
        addTab(generalSection, select = false)
        addTab(volumesSection, select = false)
        addTab(cliSection, select = false)
        addTab(envSection, select = false)
        addTab(portsSection, select = false)
        addTab(networksSection, select = false)
        addLazyTab("Other Compose Properties") { genericSection }
    }
}