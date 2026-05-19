package io.github.inductiveautomation.kindling.docker.services.generic

import io.github.inductiveautomation.kindling.docker.compose.ServiceNetworkConnectionEditor
import io.github.inductiveautomation.kindling.docker.services.ConfigSection
import io.github.inductiveautomation.kindling.docker.services.model.ServiceNetworkConnection
import io.github.inductiveautomation.kindling.utils.Action
import io.github.inductiveautomation.kindling.utils.FlatScrollPane
import io.github.inductiveautomation.kindling.utils.HorizontalSplitPane
import io.github.inductiveautomation.kindling.utils.configureCellRenderer
import io.github.inductiveautomation.kindling.utils.listCellRenderer
import io.github.inductiveautomation.kindling.utils.minSelectedIndex
import net.miginfocom.swing.MigLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities

class NetworkConnectionEditor(
    private val connections: MutableMap<String, ServiceNetworkConnection>,
    initialNetworkOptions: List<String>,
) : ConfigSection("Networks") {
    var networkOptions = initialNetworkOptions
        set(value) {
            field = value
            dropdown.model = DefaultComboBoxModel(
                value.filter {
                    it !in connections.keys
                }.toTypedArray(),
            )
            dropdown.selectedIndex = -1
        }

    private val dropdown = JComboBox<String>().apply {
        model = DefaultComboBoxModel(
            networkOptions.filter {
                it !in connections.keys
            }.toTypedArray(),
        )

        configureCellRenderer { _, value, _, _, _ ->
            text = if (value.isNullOrBlank()) "Select a network" else value
        }

        selectedIndex = -1
    }

    private val networkList = JList(DefaultListModel<Map.Entry<String, ServiceNetworkConnection>>()).apply {
        (model as DefaultListModel).addAll(connections.entries)

        cellRenderer = listCellRenderer<Map.Entry<String, ServiceNetworkConnection>> { _, value, _, _, _ ->
            text = value.key
        }
        selectionMode = ListSelectionModel.SINGLE_SELECTION
    }

    private val addButton = JButton(
        Action("+", "Add selected connection") {
            val selectedKey = dropdown.selectedItem as String? ?: return@Action
            val newOptions = networkOptions.filter {
                it != selectedKey && it !in connections.keys
            }.toTypedArray()

            connections[selectedKey] = ServiceNetworkConnection()

            dropdown.model = DefaultComboBoxModel(newOptions)
            dropdown.selectedIndex = -1

            val newEntry = connections.entries.find { it.key == selectedKey }!!

            (networkList.model as DefaultListModel).addElement(newEntry)
            networkList.selectedIndex = connections.size - 1

            fireConfigChange()
        },
    )

    private val removeButton = JButton(
        Action("-", "Remove selected connection") {
            val index = networkList.selectionModel.minSelectedIndex ?: return@Action
            val selectedEntry = (networkList.model as DefaultListModel)[index]

            connections.remove(selectedEntry.key)
            (networkList.model as DefaultListModel).removeElement(selectedEntry)

            dropdown.model = DefaultComboBoxModel(
                networkOptions.filter {
                    it !in connections.keys
                }.toTypedArray(),
            )
            dropdown.selectedIndex = -1

            fireConfigChange()
        },
    )

    private val editorArea = JPanel(MigLayout("fill, ins 4"))

    init {
        val sidebar = JPanel(MigLayout("fill, ins 4, aligny top")).apply {
            add(dropdown, "pushx, growx")
            add(removeButton)
            add(addButton, "wrap")

            add(FlatScrollPane(networkList), "push, grow, span")
        }

        add(
            HorizontalSplitPane(
                left = sidebar,
                right = FlatScrollPane(editorArea),
                resizeWeight = 0.1,
            ),
            "push, grow, span",
        )

        networkList.selectionModel.addListSelectionListener {
            val index = (it.source as ListSelectionModel).minSelectedIndex
            if (index == null) {
                editorArea.removeAll()
            } else {
                val selectedConnection = (networkList.model as DefaultListModel)[index].value
                editorArea.removeAll()
                editorArea.add(
                    ServiceNetworkConnectionEditor(selectedConnection).apply {
                        addValueChangeListener { fireConfigChange() }
                    }.component,
                    "push, grow",
                )
            }

            SwingUtilities.invokeLater {
                revalidate()
                repaint()
            }
        }
    }
}
