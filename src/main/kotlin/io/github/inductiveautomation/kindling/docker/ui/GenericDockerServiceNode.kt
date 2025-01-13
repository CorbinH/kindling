package io.github.inductiveautomation.kindling.docker.ui

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.docker.model.DefaultDockerServiceModel
import io.github.inductiveautomation.kindling.utils.tag
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import net.miginfocom.swing.MigLayout
import org.jdesktop.swingx.JXFormattedTextField

class GenericDockerServiceNode(
    override val model: DefaultDockerServiceModel = DefaultDockerServiceModel(
        DefaultDockerServiceModel.DEFAULT_GENERIC_IMAGE
    ),
) : AbstractDockerServiceNode<DefaultDockerServiceModel>() {

    private val deleteButton = JButton(FlatSVGIcon("icons/bx-x.svg").derive(12, 12))

    override val header = JPanel(MigLayout("fill, ins 0")).apply {
        add(deleteButton, "east")
    }

    init {
        add(header, "growx, spanx", 0)

        hostNameLabel.text = buildString {
            tag("html") {
                tag("b") {
                    append("Hostname: ")
                }
                if (model.hostName.isNullOrEmpty()) {
                    tag("i") {
                        append("(default)")
                    }
                } else {
                    append(model.hostName)
                }
            }
        }

        serviceNameLabel.text = buildString {
            tag("html") {
                tag("b") {
                    append("Name: ")
                }
                append(model.containerName)
            }
        }
    }

    override val configEditor: JComponent = JPanel()

    inner class GenericNodeConfigPanel : JPanel(MigLayout("fill, ins 4, gap 4, flowy")) {
        /* General */
        private val imageLabel = JLabel("Image")
        private val imageEntry = JTextField(model.image)

        private val hostLabel = JLabel("Hostname")
        private val hostEntry = JXFormattedTextField(model.hostName).apply {
            prompt = "(default)"
        }

        private val containerLabel = JLabel("Container Name")
        private val containerEntry = JTextField(model.containerName)

        private val General by configSection {
            add(imageLabel)
            add(imageEntry, "growx, wrap")
            add(hostLabel)
            add(hostEntry, "growx, wrap")
            add(containerLabel)
            add(containerEntry, "growx")
        }

        /* Port Mappings */
        private val portMappingLabel = JLabel("Add/Remove")
        private val addButton = JButton("+")
        private val removeButton = JButton("-")

//        private val portMappingTable = ReifiedJXTable()
        private val Ports by configSection {

        }

        /* Environment Variables */
        private val `Environment Variables` by configSection {

        }

        /* Command Line Arguments */
        private val `Commend Line Arguments` by configSection {

        }

        /* Volumes */
        private val Volumes by configSection {

        }

        init {
            add(General, "grow")
        }

//        private class PortMappingTableModel(
//            override val data: MutableList<PortMapping>,
//        ) : ReifiedListTableModel<PortMapping>(data, PortMappingColumns) {
//
//            object PortMappingColumns : ColumnList<PortMapping>() {
//                val Host by column {
//                    it.hostPort
//                }
//
//                val Container by column {
//                    it.containerPort
//                }
//            }
//        }
    }

    companion object {
        private fun configSection(
            title: String? = null,
            block: JPanel.() -> Unit,
        ) = ReadOnlyProperty { _: GenericNodeConfigPanel, property: KProperty<*> ->
            JPanel(MigLayout("fill, ins 2")).apply {
                border = BorderFactory.createTitledBorder(
                    BorderFactory.createEmptyBorder(),
                    title ?: property.name
                )
                block()
            }
        }

        val SERVICE_NAME_REGEX = """[a-zA-Z0-9][a-zA-Z0-9_.-]+""".toRegex()
        val IMAGE_NAME_REGEX = """.*/(?<serviceName>.*):.*""".toRegex()
    }
}
