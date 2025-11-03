package io.github.inductiveautomation.kindling.docker.ui

import io.github.inductiveautomation.kindling.docker.model.Docker.mssqlImageVersions
import io.github.inductiveautomation.kindling.docker.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.model.DockerVolume
import io.github.inductiveautomation.kindling.docker.ui.GenericDockerServiceNode.Companion.SERVICE_NAME_REGEX
import io.github.inductiveautomation.kindling.docker.ui.editors.CliArgumentsEditor
import io.github.inductiveautomation.kindling.docker.ui.editors.GenericEditor
import io.github.inductiveautomation.kindling.docker.ui.editors.MSSQLEnvVariablesEditor
import io.github.inductiveautomation.kindling.docker.ui.editors.NetworkEditor
import io.github.inductiveautomation.kindling.docker.ui.editors.PortMappingEditor
import io.github.inductiveautomation.kindling.docker.ui.editors.VolumeEditor
import io.github.inductiveautomation.kindling.utils.EDT_SCOPE
import io.github.inductiveautomation.kindling.utils.RegexInputVerifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jdesktop.swingx.JXFormattedTextField
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JTextField

class MSSQLNodeConfigPanel(
    override val node: MSSQLServiceNode,
    volumeOptions: List<DockerVolume>,
    networkOptions: List<DockerNetwork>,
) : NodeConfigPanel() {
    /* General */
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

    private val versionLabel = JLabel("Version")
    private val versionDropdown = JComboBox<String>()

    init {
        EDT_SCOPE.launch {
            val options = withContext(Dispatchers.IO) {
                mssqlImageVersions.await()
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
            add(imageLabel)
            add(imageEntry, "growx, wrap")
            add(hostLabel)
            add(hostEntry, "growx, wrap")
            add(containerLabel)
            add(containerEntry, "growx, wrap")
            add(versionLabel)
            add(versionDropdown, "growx, wrap")
        }
    }

    override val portsSection = PortMappingEditor(node.model.ports).bind()
    override val envSection = MSSQLEnvVariablesEditor(node.model.environment, node.model.version).bind()
    override val cliSection = CliArgumentsEditor(node.model.commands).bind()
    override val volumesSection = VolumeEditor(node.model.volumes, volumeOptions).bind()
    override val networksSection = NetworkEditor(node.model.networks, networkOptions).bind()
    override val genericSection by lazy {
        GenericEditor(node.model).bind()
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

    fun resetNames() {
        hostEntry.text = node.model.hostName
        containerEntry.text = node.model.containerName
    }
}
