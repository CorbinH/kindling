package io.github.inductiveautomation.kindling.docker.services.generic

import com.formdev.flatlaf.extras.components.FlatSplitPane
import com.formdev.flatlaf.extras.components.FlatTextField
import io.github.inductiveautomation.kindling.docker.services.ConfigSection
import io.github.inductiveautomation.kindling.docker.services.model.PortMapping
import io.github.inductiveautomation.kindling.utils.ColumnList
import io.github.inductiveautomation.kindling.utils.FlatScrollPane
import io.github.inductiveautomation.kindling.utils.HorizontalSplitPane
import io.github.inductiveautomation.kindling.utils.ReifiedJXTable
import io.github.inductiveautomation.kindling.utils.ReifiedListTableModel
import io.github.inductiveautomation.kindling.utils.StyledLabel
import io.github.inductiveautomation.kindling.utils.attachValidator
import net.miginfocom.swing.MigLayout
import java.awt.Component
import java.awt.Font
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableCellRenderer
import kotlin.properties.Delegates

class PortMappingEditor(
    val initialData: MutableList<PortMapping>,
) : ConfigSection("Ports") {

    enum class State {
        EDITING {
            override val portMappingText: String = "Editing Port Mapping"
            override val createButtonText: String = "Save"
        },
        CREATING {
            override val portMappingText: String = "Creating Port Mapping"
            override val createButtonText: String = "Create"
        }, ;

        abstract val portMappingText: String
        abstract val createButtonText: String
    }

    private var state: State = State.EDITING
        set(value) {
            field = value
            portEditingPanel.apply {
                headerLabel.isVisible = true
                headerLabel.text = value.portMappingText
                createButton.text = value.createButtonText
            }
        }

    private val portMappingTable = ReifiedJXTable(
        ReifiedListTableModel(
            initialData,
            PortMappingTableColumns,
        ),
    ).apply {
        isColumnControlVisible = false
    }

    private val portTablePanel = JPanel(MigLayout("fill, gap 5")).apply {
        add(FlatScrollPane(portMappingTable), "grow, push, span 2, wrap")
        val deleteButton = JButton("Delete").apply {
            isEnabled = false
            addActionListener {
                portMappingTable.apply {
                    val currentSelectedRow = selectedRow
                    selectionModel.clearSelection()
                    initialData.removeAt(currentSelectedRow)
                    model.fireTableRowsDeleted(currentSelectedRow, currentSelectedRow)
                }
                splitPane.setDividerLocation(1.0)
            }
        }

        add(deleteButton, "alignx leading")

        portMappingTable.selectionModel.addListSelectionListener {
            deleteButton.isEnabled = !(it.source as ListSelectionModel).isSelectionEmpty
        }

        val addButton = JButton("Add").apply {
            addActionListener {
                portMappingTable.selectionModel.clearSelection()
                portEditingPanel.apply {
                    portMapping = PortMapping("", "")
                    targetEntry.requestFocus()
                    state = State.CREATING
                    createButton.text = "Create"
                }
                if (splitPane.dividerSize + splitPane.dividerLocation >= splitPane.width) {
                    splitPane.setDividerLocation(DEFAULT_DIVIDER_LOCATION)
                }
            }
        }
        add(addButton, "alignx trailing")
    }

    private val portEditingPanel = PortEditingPanel()

    private val splitPane: FlatSplitPane = HorizontalSplitPane(
        left = portTablePanel,
        right = portEditingPanel,
        resizeWeight = 0.5,
        expandableSide = FlatSplitPane.ExpandableSide.left,
    )

    init {
        add(splitPane, "grow")

        portMappingTable.apply {
            model.addTableModelListener {
                fireConfigChange()
            }
            selectionModel.addListSelectionListener {
                portEditingPanel.apply {
                    portMapping = portMappingTable.model.data[it.firstIndex]
                    state = State.EDITING
                    createButton.text = "Save"
                }
                if (splitPane.dividerSize + splitPane.dividerLocation >= splitPane.width) {
                    splitPane.setDividerLocation(DEFAULT_DIVIDER_LOCATION)
                }
            }
        }

        SwingUtilities.invokeLater {
            splitPane.setDividerLocation(1.0)
        }
    }

    private inner class PortEditingPanel : JPanel(MigLayout("fill, flowy")) {
        var portMapping: PortMapping? by Delegates.observable(null) { _, _, newValue ->
            if (newValue != null) {
                targetEntry.text = newValue.target
                publishedEntry.text = newValue.published
                nameEntry.text = newValue.name
                hostIpEntry.text = newValue.hostIp
                modeEntry.selectedItem = newValue.mode
                protocolEntry.selectedItem = newValue.protocol
                appProtocolEntry.text = newValue.appProtocol
            }
        }

        val headerLabel = JLabel("").apply {
            font = font.deriveFont(Font.BOLD, 16f)
            isVisible = false
        }

        val configurationCheckbox = JCheckBox("Complex").apply {
            addActionListener {
                innerPanel.isVisible = !innerPanel.isVisible
            }
        }

        val targetLabel = StyledLabel {
            add("Target", Font.BOLD)
        }

        val targetEntry = FlatTextField().apply {
            toolTipText = "The container port."
            attachValidator {
                it?.toUShortOrNull() != null
            }
            name = "Target"
        }

        val publishedLabel = StyledLabel {
            add("Published", Font.BOLD)
        }

        val publishedEntry = FlatTextField().apply {
            toolTipText = "<html>The publicly exposed port. " +
                "It is defined as a string and can be set as a range using syntax <code>start-end.</code><br>" +
                "It means the actual port is assigned a remaining available port, within the set range.</html>"
            attachValidator { s ->
                val strings = s?.split("-") ?: return@attachValidator false
                strings.size <= 2 && strings.all {
                    it.toUShortOrNull() != null
                }
            }
            name = "Published"
        }

        val nameLabel = StyledLabel {
            add("Name ", Font.BOLD)
            add("Required", "superscript")
        }

        val nameEntry = FlatTextField().apply {
            toolTipText = "A human-readable name for the port, used to document its usage within the service."
            attachValidator {
                !it.isNullOrEmpty()
            }
            name = "Name"
        }

        val hostIpLabel = StyledLabel {
            add("Host IP", Font.BOLD)
        }
        val hostIpEntry = FlatTextField().apply {
            toolTipText = "<html>The host IP mapping. If it is not set, it binds to all network interfaces (<code>0.0.0.0</code>).</html>"
            text = "0.0.0.0"
            attachValidator {
                val regex = """^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)${'$'}""".toRegex()
                it != null && regex.containsMatchIn(it)
            }
            name = "Host IP"
        }

        val protocolLabel = StyledLabel {
            add("Protocol", Font.BOLD)
        }
        val protocolEntry = JComboBox(arrayOf("tcp", "udp")).apply {
            toolTipText = "<html>The port protocol (<code>tcp</code> or <code>udp</code>). Defaults to <code>tcp</code>.</html>"
            name = "Protocol"
        }

        val appProtocolLabel = StyledLabel {
            add("App Protocol", Font.BOLD)
        }
        val appProtocolEntry = JTextField().apply {
            toolTipText = "The application protocol (TCP/IP level 4 / OSI level 7) this port is used for.\n" +
                "This is optional and can be used as a hint for Compose to offer richer behavior for protocols that it understands.\n" +
                "Introduced in Docker Compose version 2.26.0."
            name = "App Protocol"
        }

        val modeLabel = StyledLabel {
            add("Mode", Font.BOLD)
        }
        val modeEntry = JComboBox(arrayOf("ingress", "host")).apply {
            toolTipText = "<html>Specifies how the port is published in a Swarm setup.<br>" +
                "If set to <code>host</code>, it publishes the port on every node in Swarm.<br>" +
                "If set to <code>ingress</code>, it allows load balancing across the nodes in Swarm. Defaults to <code>ingress</code>.<html>"
            name = "Mode"
        }

        val gap = "gapbottom 10"

        val innerPanel = JPanel(MigLayout("fillx, flowy, hidemode 3")).apply {
            add(nameLabel)
            add(nameEntry, "grow, $gap")
            add(hostIpLabel)
            add(hostIpEntry, "grow, $gap")
            add(modeLabel)
            add(modeEntry, "grow, $gap")
            add(protocolLabel)
            add(protocolEntry, "grow, $gap")
            add(appProtocolLabel)
            add(appProtocolEntry, "grow, $gap")
            isVisible = false
        }

        private fun validateAllComponents(): List<String> {
            val list: List<FlatTextField> = buildList {
                add(publishedEntry)
                add(targetEntry)
                if (configurationCheckbox.isSelected) {
                    add(nameEntry)
                    add(hostIpEntry)
                }
            }

            return list.mapNotNull {
                if (!it.inputVerifier.verify(it)) it.name else null
            }
        }

        private fun applyChanges() {
            portMapping?.apply {
                if ((configurationCheckbox.isSelected)) {
                    target = targetEntry.text
                    published = publishedEntry.text
                    name = nameEntry.text?.ifEmpty { null }
                    hostIp = hostIpEntry.text
                    protocol = protocolEntry.selectedItem as String
                    appProtocol = appProtocolEntry.text?.ifEmpty { null }
                    mode = modeEntry.selectedItem as String
                } else {
                    target = targetEntry.text
                    published = publishedEntry.text
                    name = null
                    hostIp = PortMapping.DEFAULT_HOST_IP
                    protocol = PortMapping.DEFAULT_PROTOCOL
                    appProtocol = null
                    mode = PortMapping.DEFAULT_MODE
                }
            }
        }

        private fun showFieldError(names: List<String>) {
            JOptionPane.showMessageDialog(
                null,
                names.joinToString("\n", prefix = "Port mapping fields are invalid:\n"),
                "Port mapping error!",
                JOptionPane.ERROR_MESSAGE,
            )
        }

        val createButton = JButton("Create").apply {
            addActionListener {
                val names = validateAllComponents()
                when (state) {
                    State.EDITING -> {
                        if (names.isEmpty()) {
                            applyChanges()
                            portMappingTable.model.fireTableDataChanged()
                        } else {
                            showFieldError(names)
                        }
                    }
                    State.CREATING -> {
                        if (names.isEmpty()) {
                            applyChanges()
                            portMapping?.let { initialData.add(it) }
                            portMappingTable.model.fireTableDataChanged()
                            splitPane.setDividerLocation(1.0)
                        } else {
                            showFieldError(names)
                        }
                    }
                }
            }
        }

        init {
            add(headerLabel)
            add(publishedLabel)
            add(publishedEntry, "grow, $gap")
            add(targetLabel)
            add(targetEntry, "grow, $gap")
            add(configurationCheckbox)
            add(innerPanel, "grow, push, $gap")
            add(createButton, "alignx trailing, aligny bottom")
        }
    }

    companion object {
        private const val DEFAULT_DIVIDER_LOCATION = 0.6
    }
}

@Suppress("unused")
private object PortMappingTableColumns : ColumnList<PortMapping>() {
    val Name by column(
        value = PortMapping::name,
        column = {
            cellRenderer = object : DefaultTableCellRenderer() {
                override fun getTableCellRendererComponent(
                    table: JTable?,
                    value: Any?,
                    isSelected: Boolean,
                    hasFocus: Boolean,
                    row: Int,
                    column: Int,
                ): Component? {
                    this.horizontalAlignment = CENTER
                    return super.getTableCellRendererComponent(table, value ?: "-", isSelected, hasFocus, row, column)
                }
            }
        },
    )
    val Published by column {
        it.published
    }
    val Target by column {
        it.target
    }
}
