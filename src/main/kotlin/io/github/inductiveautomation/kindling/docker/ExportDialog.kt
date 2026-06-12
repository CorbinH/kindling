package io.github.inductiveautomation.kindling.docker

import io.github.inductiveautomation.kindling.utils.tag
import net.miginfocom.swing.MigLayout
import java.awt.Component
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.SwingUtilities

sealed interface ExportMode {
    data object Standalone : ExportMode
    data class Bundle(val placement: GwbkPlacement) : ExportMode
}

enum class GwbkPlacement { COPY, MOVE }

private class ExportDialog(parent: Component?) :
    JDialog(
        SwingUtilities.getWindowAncestor(parent),
        "Export as...",
        ModalityType.APPLICATION_MODAL,
    ) {
    var result: ExportMode? = null
        private set

    private val standaloneRadio = JRadioButton(
        modeLabel("Standalone", "Exports a single docker-compose.yaml at the path you choose.", separator = "<br>"),
        true,
    )
    private val bundleRadio = JRadioButton(
        modeLabel("Bundle", "Exports docker-compose.yaml plus all GWBKs into a folder.", separator = "<br>"),
    )

    private val copyRadio = JRadioButton(
        modeLabel("Copy", "Copy all GWBK files to this folder", separator = " - "),
        true,
    )
    private val moveRadio = JRadioButton(
        modeLabel("Move", "Move all GWBK files to this folder", separator = " - "),
    )

    init {
        ButtonGroup().apply {
            add(standaloneRadio)
            add(bundleRadio)
        }
        ButtonGroup().apply {
            add(copyRadio)
            add(moveRadio)
        }

        val placementPanel = JPanel(MigLayout("ins 0")).apply {
            add(copyRadio)
            add(moveRadio)
        }

        fun updatePlacementEnabled() {
            val enabled = bundleRadio.isSelected
            copyRadio.isEnabled = enabled
            moveRadio.isEnabled = enabled
        }
        updatePlacementEnabled()
        bundleRadio.addItemListener { updatePlacementEnabled() }
        standaloneRadio.addItemListener { updatePlacementEnabled() }

        val cancelButton = JButton("Cancel").apply {
            addActionListener {
                result = null
                dispose()
            }
        }
        val nextButton = JButton("Next...").apply {
            addActionListener {
                result = if (standaloneRadio.isSelected) {
                    ExportMode.Standalone
                } else {
                    val placement = if (moveRadio.isSelected) GwbkPlacement.MOVE else GwbkPlacement.COPY
                    ExportMode.Bundle(placement)
                }
                dispose()
            }
        }

        contentPane = JPanel(MigLayout("fill, ins 10, wrap 1")).apply {
            add(standaloneRadio, "growx")
            add(bundleRadio, "growx")
            add(placementPanel, "growx, gapleft 25")
            add(
                JPanel(MigLayout("ins 0, fill")).apply {
                    add(cancelButton, "split 2, align right")
                    add(nextButton)
                },
                "growx",
            )
        }

        pack()
        setLocationRelativeTo(parent)
        defaultCloseOperation = DISPOSE_ON_CLOSE
    }
}

fun showExportDialog(parent: Component?): ExportMode? {
    val dialog = ExportDialog(parent)
    dialog.isVisible = true
    return dialog.result
}

private fun modeLabel(title: String, description: String, separator: String): String = buildString {
    tag("html") {
        tag("b", content = title)
        append(separator)
        append(description)
    }
}
