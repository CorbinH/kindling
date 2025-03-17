package io.github.inductiveautomation.kindling.docker.ui

import io.github.inductiveautomation.kindling.docker.model.Docker.ignitionImageVersions
import io.github.inductiveautomation.kindling.docker.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.model.DockerVolume
import io.github.inductiveautomation.kindling.docker.ui.GenericDockerServiceNode.Companion.SERVICE_NAME_REGEX
import io.github.inductiveautomation.kindling.docker.ui.editors.GatewayCliArgEditor
import io.github.inductiveautomation.kindling.docker.ui.editors.GatewayEnvVariablesEditor
import io.github.inductiveautomation.kindling.docker.ui.editors.NetworkEditor
import io.github.inductiveautomation.kindling.docker.ui.editors.PortMappingEditor
import io.github.inductiveautomation.kindling.docker.ui.editors.VolumeEditor
import io.github.inductiveautomation.kindling.utils.EDT_SCOPE
import io.github.inductiveautomation.kindling.utils.RegexInputVerifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jdesktop.swingx.JXFormattedTextField
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JTextField

class GatewayNodeConfigPanel(
    override val node: GatewayServiceNode,
    volumeOptions: List<DockerVolume>,
    networkOptions: List<DockerNetwork>,
) : NodeConfigPanel("fill, ins 4, flowy") {
    private val imageLabel = JLabel("Image")
    private val imageEntry = JTextField(node.model.image).apply {
        isEnabled = false
        isFocusable = false

        node.addServiceModelChangeListener {
            text = node.model.image
        }
    }

    private val hostLabel = JLabel("Hostname")
    private val hostEntry = JXFormattedTextField("(default)").apply {
        node.model.hostName?.let {
            text = it
        }
        addActionListener {
            node.model.hostName = text
            node.fireServiceModelChangedEvent()
        }
        addFocusListener(
            object : FocusListener {
                override fun focusLost(e: FocusEvent) {
                    node.model.hostName = text
                    node.fireServiceModelChangedEvent()
                }

                override fun focusGained(e: FocusEvent) {
                }
            },
        )
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
        addFocusListener(
            object : FocusListener {
                override fun focusLost(e: FocusEvent) {
                    if (SERVICE_NAME_REGEX.matches(text)) {
                        node.model.containerName = text
                        node.fireServiceModelChangedEvent()
                    }
                }

                override fun focusGained(e: FocusEvent) {
                }
            },
        )
    }

    private val imageTypeLabel = JLabel("Image Type")
    private val imageTypeEntry = JComboBox(arrayOf("ignition")).apply {
        isEnabled = false
        toolTipText = "Support for kcollins images will be available in a future release of Kindling."
    }

    private val versionLabel = JLabel("Version")
    private val versionDropdown = JComboBox<String>()

    init {
        EDT_SCOPE.launch {
            val options = withContext(Dispatchers.IO) {
                ignitionImageVersions.await()
            }

            versionDropdown.model = DefaultComboBoxModel(options.toTypedArray())
            versionDropdown.selectedItem = node.model.version

            versionDropdown.addActionListener {
                node.model.version = versionDropdown.selectedItem as String
                envSection.version = node.model.version
                node.fireServiceModelChangedEvent()
            }
        }

        node.addServiceModelChangeListener {
            envSection.updateData()
        }
    }

    override val generalSection = object : ConfigSection("General", "fillx, ins 0, aligny top") {
        init {
            add(imageLabel, "sg")
            add(imageEntry, "growx, wrap")
            add(hostLabel, "sg")
            add(hostEntry, "growx, wrap")
            add(containerLabel, "sg")
            add(containerEntry, "growx, wrap")
            add(imageTypeLabel, "sg")
            add(imageTypeEntry, "grow, wrap")
            add(versionLabel, "sg")
            add(versionDropdown, "growx, wrap")
        }
    }

    override val envSection = GatewayEnvVariablesEditor(node.model.environment, node.model.version).bind()
    override val portsSection = PortMappingEditor(node.model.ports).bind()
    override val cliSection = GatewayCliArgEditor(node.model.commands).bind()
    override val volumesSection = VolumeEditor(node.model.volumes, volumeOptions).bind()
    override val networksSection = NetworkEditor(node.model.networks, networkOptions).bind()

    var volumeOptions: List<DockerVolume> by volumesSection::volumeOptions
    var networkOptions: List<DockerNetwork> by networksSection::networkOptions

    init {
        add(generalSection, "grow")
        add(cliSection, "grow, spany 2, wrap")
        add(envSection, "grow, spany")
        add(portsSection, "grow, sg")
        add(volumesSection, "grow, sg")
        add(networksSection, "grow, sg")
    }

    fun resetNames() {
        hostEntry.text = node.model.hostName
        containerEntry.text = node.model.containerName
    }
}
