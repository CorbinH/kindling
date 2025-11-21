package io.github.inductiveautomation.kindling.docker.compose

import io.github.inductiveautomation.kindling.docker.networks.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.networks.model.DockerNetwork.Ipam
import io.github.inductiveautomation.kindling.utils.Action
import io.github.inductiveautomation.kindling.utils.ColumnList
import io.github.inductiveautomation.kindling.utils.DocumentAdapter
import io.github.inductiveautomation.kindling.utils.FlatScrollPane
import io.github.inductiveautomation.kindling.utils.ReifiedJXTable
import io.github.inductiveautomation.kindling.utils.ReifiedMapTableModel
import io.github.inductiveautomation.kindling.utils.minSelectedIndex
import io.github.inductiveautomation.kindling.utils.onChange
import io.github.inductiveautomation.kindling.utils.text
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.ListSelectionModel
import net.miginfocom.swing.MigLayout
import org.jdesktop.swingx.JXTextArea

@Suppress("unused")
class NetworkEditor(
    data: DockerNetwork,
) : ComposeObjectEditor<DockerNetwork>("Network", data), RootEditor {
    val networkName by text("Name", value = data.name) { data.name = it }
    val driver by text(value = data.driver) { data.driver = it }
    val attachable by checkbox(value = data.attachable) { data.attachable = it }
    val driverOpts by map("Driver Opts", data.driverOpts)
    val enableIpv4 by checkbox("Enable Ipv4", data.enableIpv4) { data.enableIpv4 = it }
    val enableIpv6 by checkbox("Enable Ipv6", data.enableIpv6) { data.enableIpv6 = it }
    val external by checkbox(value = data.external) { data.external = it }
    val internal by checkbox(value = data.internal) { data.internal = it }
    val labels by map(value = data.labels)
    val ipam by composeObject(IpamEditor(data.ipam))

    inner class IpamEditor(data: Ipam) : ComposeObjectEditor<Ipam>("IPAM", data) {
        val driver by text(value = data.driver) { data.driver = it }
        val options by map(value = data.options)
        val config by list(
            value = data.config,
            createDefault = { Ipam.IpamConfig("", "", "", mutableMapOf()) },
            configure = {
                val subnetLabel = JLabel("Subnet")
                val subnetEntry = JTextArea(element.subnet)

                val ipRangeLabel = JLabel("IP Range")
                val ipRangeEntry = JTextArea(element.ipRange)

                val gatewayLabel = JLabel("Gateway")
                val gatewayEntry = JTextArea(element.gateway)

                /* Table */
                val hostEntry = JXTextArea("Host")
                val ipEntry = JXTextArea("IP")

                val auxAddressLabel = JLabel("AUX Addresses")
                val auxAddressTable = ReifiedJXTable(ReifiedMapTableModel(element.auxAddresses, AuxAddressColumns)).apply {
                    isColumnControlVisible = false
                    selectionMode = ListSelectionModel.SINGLE_SELECTION
                }

                val addButton = JButton(
                    Action("+", "Add new element") {
                        if (hostEntry.text.isNotBlank() && ipEntry.text.isNotBlank()) {
                            element.auxAddresses[hostEntry.text] = ipEntry.text
                            auxAddressTable.model.fireTableDataChanged()
                        }
                    }
                )

                val removeButton = JButton(
                    Action("-", "Remove selected element") {
                        val viewIndex = auxAddressTable.selectionModel.minSelectedIndex ?: return@Action
                        val modelIndex = auxAddressTable.convertRowIndexToModel(viewIndex)

                        val key = auxAddressTable.model.getValueAt(modelIndex, 0) as String

                        element.auxAddresses.remove(key)
                        auxAddressTable.model.fireTableDataChanged()
                    }
                )

                auxAddressTable.selectionModel.addListSelectionListener {
                    removeButton.isEnabled = !(it.source as ListSelectionModel).isSelectionEmpty
                }

                hostEntry.document.addDocumentListener(
                    DocumentAdapter {
                        addButton.isEnabled = it.document.text.isNotBlank() && ipEntry.text.isNotBlank()
                    }
                )

                ipEntry.document.addDocumentListener(
                    DocumentAdapter {
                        addButton.isEnabled = it.document.text.isNotBlank() && hostEntry.text.isNotBlank()
                    }
                )

                subnetEntry.document.onChange {
                    element.subnet = it
                    root?.fireValueChanged()
                }

                ipRangeEntry.document.onChange {
                    element.ipRange = it
                    root?.fireValueChanged()
                }

                gatewayEntry.document.onChange {
                    element.gateway = it
                    root?.fireValueChanged()
                }

                auxAddressTable.model.addTableModelListener {
                    root?.fireValueChanged()
                }

                val tableEntry = JPanel(MigLayout("fillx, ins 0")).apply {
                    add(hostEntry, "pushx, growx, sg, gapright 10")
                    add(ipEntry, "pushx, growx, sg, gapright 10")
                    add(removeButton)
                    add(addButton)
                }

                /* Add components */
                add(subnetLabel, "wrap")
                add(subnetEntry, "growx, wrap")
                add(ipRangeLabel, "wrap")
                add(ipRangeEntry, "growx, wrap")
                add(gatewayLabel, "wrap")
                add(gatewayEntry, "growx, wrap")
                add(auxAddressLabel, "growx, wrap")
                add(tableEntry, "growx, wrap")
                add(FlatScrollPane(auxAddressTable), "push, grow")
            }
        )
    }

    private val listeners: MutableList<RootEditor.ValueChangeListener> = mutableListOf()

    override fun addValueChangeListener(l: RootEditor.ValueChangeListener) {
        listeners.add(l)
    }

    override fun fireValueChanged() {
        listeners.forEach {
            it.valueChange()
        }
    }
}

@Suppress("unused")
object AuxAddressColumns : ColumnList<Map.Entry<String, String>>() {
    val Host by column {
        it.key
    }

    val IP by column {
        it.value
    }
}
