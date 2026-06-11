package io.github.inductiveautomation.kindling.docker.services.ignition

import io.github.inductiveautomation.kindling.docker.services.ConfigSection
import io.github.inductiveautomation.kindling.docker.services.NodeConfigPanel
import io.github.inductiveautomation.kindling.docker.services.generic.ComposeEditor
import io.github.inductiveautomation.kindling.docker.services.generic.GenericDockerServiceNode
import io.github.inductiveautomation.kindling.docker.services.generic.NetworkConnectionEditor
import io.github.inductiveautomation.kindling.docker.services.generic.PortMappingEditor
import io.github.inductiveautomation.kindling.docker.services.generic.VolumeEditor
import io.github.inductiveautomation.kindling.docker.services.ignition.IgnitionServiceTool.ignitionImageVersions
import io.github.inductiveautomation.kindling.docker.services.ignition.model.IgnitionCommandLineArgument
import io.github.inductiveautomation.kindling.docker.volumes.model.DockerVolume
import io.github.inductiveautomation.kindling.utils.EDT_SCOPE
import io.github.inductiveautomation.kindling.utils.RegexInputVerifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jdesktop.swingx.JXFormattedTextField
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JTextField

class IgnitionNodeConfigPanel(
    override val node: IgnitionServiceNode,
    volumeOptions: List<DockerVolume>,
    networkOptions: List<String>,
) : NodeConfigPanel() {
    private val imageLabel = JLabel("Image")
    private val imageEntry = JTextField(node.model.image).apply {
        isEnabled = false
        isFocusable = false

        node.model.addServiceModelChangeListener {
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
            node.model.fireServiceModelChangedEvent()
        }
        addFocusListener(
            object : FocusAdapter() {
                override fun focusLost(e: FocusEvent) {
                    node.model.hostName = text
                    node.model.fireServiceModelChangedEvent()
                }
            },
        )
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
        addFocusListener(
            object : FocusAdapter() {
                override fun focusLost(e: FocusEvent) {
                    if (GenericDockerServiceNode.SERVICE_NAME_REGEX.matches(text)) {
                        node.model.containerName = text
                        node.model.fireServiceModelChangedEvent()
                    }
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
                node.model.fireServiceModelChangedEvent()
            }
        }

        node.model.addServiceModelChangeListener {
            envSection.updateData()
            setEnabledAt(indexOfComponent(gwbkSection), hasGwbk())
        }
    }

    private fun hasGwbk(): Boolean {
        val restoreFlag = IgnitionCommandLineArgument.GWBK_RESTORE_PATH.flag
        return node.model.commands.any { it.startsWith("$restoreFlag ") }
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

    override val envSection = IgnitionEnvironmentVariablesEditor(node.model.environment, node.model.version).bind()
    override val portsSection = PortMappingEditor(node.model.ports).bind()
    override val cliSection = IgnitionCliArgEditor(node.model.commands).bind()
    override val volumesSection = VolumeEditor(node.model.volumes, volumeOptions).bind()
    override val networksSection = NetworkConnectionEditor(node.model.networks, networkOptions).bind()
    private val gwbkSection = GatewayBackupEditor(node.model).bind()
    override val genericSection by lazy {
        ComposeEditor(node.model).bind()
    }

    var volumeOptions: List<DockerVolume> by volumesSection::volumeOptions
    var networkOptions: List<String> by networksSection::networkOptions

    init {
        addTab(generalSection, select = false)
        addTab(volumesSection, select = false)
        addTab(cliSection, select = false)
        addTab(envSection, select = false)
        addTab(portsSection, select = false)
        addTab(networksSection, select = false)
        addTab(gwbkSection, select = false)
        setEnabledAt(indexOfComponent(gwbkSection), hasGwbk())
        addLazyTab("Other Compose Properties") { genericSection }
    }

    fun resetNames() {
        hostEntry.text = node.model.hostName
        containerEntry.text = node.model.containerName
    }
}
