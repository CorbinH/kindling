package io.github.inductiveautomation.kindling.docker.services.ignition

import io.github.inductiveautomation.kindling.utils.tag
import java.awt.Component
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.SwingUtilities
import net.miginfocom.swing.MigLayout

enum class PortStrategy { KEEP, RESET }

private class PortConflictDialog(
    parent: Component?,
    httpPort: String,
    httpsPort: String,
    forceSecureRedirect: Boolean,
) : JDialog(
    SwingUtilities.getWindowAncestor(parent),
    "Non-default Port Configuration",
    ModalityType.APPLICATION_MODAL,
) {
    var result: PortStrategy? = null
        private set

    private val keepRadio = JRadioButton(
        strategyLabel("Keep", "Bind the gateway's existing HTTP port."),
        true,
    )
    private val resetRadio = JRadioButton(
        strategyLabel(
            "Reset",
            "Add GATEWAY_HTTP_PORT and GATEWAY_HTTPS_PORT env vars to reset the gateway to defaults (8088/8043). HTTP will be bound to 8088.",
        ),
    )

    init {
        ButtonGroup().apply {
            add(keepRadio)
            add(resetRadio)
        }

        val header = JLabel(
            buildString {
                tag("html") {
                    tag("b", content = "Non-default port configuration detected")
                    append("<br>The selected GWBK has non-default web server settings.")
                }
            },
        )

        val detailsPanel = JPanel(MigLayout("ins 0, wrap 1")).apply {
            add(JLabel(detailLine("HTTP port", httpPort, "8088")))
            add(JLabel(detailLine("HTTPS port", httpsPort, "8043")))
            add(JLabel(detailLine("Force Secure Redirect", forceSecureRedirect.toString(), "false")))
        }

        val redirectNotice = JLabel(
            buildString {
                tag("html") {
                    tag("i") {
                        append("Force Secure Redirect is enabled — the HTTPS port will also be published regardless of which option you choose, so you can connect via HTTPS directly.")
                    }
                }
            },
        )

        val cancelButton = JButton("Cancel").apply {
            addActionListener {
                result = null
                dispose()
            }
        }
        val okButton = JButton("OK").apply {
            addActionListener {
                result = if (resetRadio.isSelected) PortStrategy.RESET else PortStrategy.KEEP
                dispose()
            }
        }

        contentPane = JPanel(MigLayout("fill, ins 10, wrap 1")).apply {
            add(header, "growx")
            add(detailsPanel, "growx, gapleft 10")
            add(keepRadio, "growx")
            add(resetRadio, "growx")
            if (forceSecureRedirect) {
                add(redirectNotice, "growx, gapleft 10")
            }
            add(
                JPanel(MigLayout("ins 0, fill")).apply {
                    add(cancelButton, "split 2, align right")
                    add(okButton)
                },
                "growx",
            )
        }

        pack()
        setLocationRelativeTo(parent)
        defaultCloseOperation = DISPOSE_ON_CLOSE
    }
}

fun showPortConflictDialog(
    parent: Component?,
    httpPort: String,
    httpsPort: String,
    forceSecureRedirect: Boolean,
): PortStrategy? {
    val dialog = PortConflictDialog(parent, httpPort, httpsPort, forceSecureRedirect)
    dialog.isVisible = true
    return dialog.result
}

private fun strategyLabel(title: String, description: String): String = buildString {
    tag("html") {
        tag("b", content = title)
        append("<br>")
        append(description)
    }
}

private fun detailLine(name: String, value: String, default: String): String = buildString {
    tag("html") {
        tag("b", content = "$name: ")
        append(value)
        if (value != default) {
            append(" ")
            tag("i", content = "(default: $default)")
        }
    }
}
