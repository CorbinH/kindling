package io.github.inductiveautomation.kindling.docker.error

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.utils.FlatScrollPane
import io.github.inductiveautomation.kindling.utils.listCellRenderer
import io.github.inductiveautomation.kindling.utils.tag
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.UIManager

class ErrorsTab(registry: ErrorRegistry) : JPanel(MigLayout("fill, ins 4")) {
    private val titleLabel = JLabel("Errors").apply {
        font = font.deriveFont(Font.BOLD, 16F)
    }

    private val listModel = DefaultListModel<DockerError>()

    private val errorList = JList(listModel).apply {
        cellRenderer = listCellRenderer<DockerError> { _, value, _, _, _ ->
            icon = value.severity.icon()
            text = buildString {
                tag("html") {
                    append(value.message)
                    if (value.relatedContainerNames.isNotEmpty()) {
                        tag("br") { }
                        tag("i") {
                            append("(")
                            append(value.relatedContainerNames.joinToString(", "))
                            append(")")
                        }
                    }
                }
            }
        }

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                val list = e?.source as? JList<*> ?: return
                if (e.clickCount == 2) {
                    val i = list.locationToIndex(e.point)

                    val element = list.model.getElementAt(i) as DockerError?

                    if (element != null) {
                        element.openConfig()
                    }
                }
            }
        })
    }

    init {
        add(titleLabel, "wrap")
        add(FlatScrollPane(errorList), "push, grow, span")

        registry.addErrorsChangedListener { errors ->
            listModel.clear()
            listModel.addAll(errors)
        }
    }

    companion object {
        private val ERROR_ICON: FlatSVGIcon = FlatSVGIcon("icons/bx-error.svg").apply {
            colorFilter = FlatSVGIcon.ColorFilter {
                UIManager.getColor("Component.error.focusedBorderColor") ?: Color.RED
            }
        }
        private val WARNING_ICON: FlatSVGIcon = FlatSVGIcon("icons/bx-warn.svg").apply {
            colorFilter = FlatSVGIcon.ColorFilter {
                UIManager.getColor("Component.warning.focusedBorderColor") ?: Color.ORANGE
            }
        }

        private fun DockerError.Severity.icon(): FlatSVGIcon = when (this) {
            DockerError.Severity.ERROR -> ERROR_ICON
            DockerError.Severity.WARNING -> WARNING_ICON
        }
    }
}
