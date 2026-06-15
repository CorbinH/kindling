package io.github.inductiveautomation.kindling.docker.services.ignition

import io.github.inductiveautomation.kindling.docker.services.ConfigSection
import io.github.inductiveautomation.kindling.docker.services.ignition.model.IgnitionCommandLineArgument
import io.github.inductiveautomation.kindling.docker.services.ignition.model.IgnitionServiceModel
import io.github.inductiveautomation.kindling.docker.services.ignition.model.IgnitionStaticDefinition
import io.github.inductiveautomation.kindling.docker.volumes.model.BindMount
import io.github.inductiveautomation.kindling.statistics.GatewayBackup
import io.github.inductiveautomation.kindling.utils.tag
import java.awt.Font
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JTextField
import javax.swing.SwingConstants
import kotlin.io.path.Path
import kotlin.io.path.exists

class GatewayBackupEditor(private val model: IgnitionServiceModel) : ConfigSection("GWBK", "fillx, ins 5, wrap 1, gapy 6") {
    private val restoreFlag = IgnitionCommandLineArgument.GWBK_RESTORE_PATH.flag
    private val httpKey = IgnitionStaticDefinition.GATEWAY_HTTP_PORT.name
    private val httpsKey = IgnitionStaticDefinition.GATEWAY_HTTPS_PORT.name

    private val portsDisplay = JLabel().apply { verticalAlignment = SwingConstants.TOP }

    private val pathField = JTextField().apply {
        addActionListener { commitPath() }
        addFocusListener(
            object : FocusAdapter() {
                override fun focusLost(e: FocusEvent) = commitPath()
            },
        )
    }

    private val xmlDisplay = JLabel().apply { verticalAlignment = SwingConstants.TOP }

    private val resetButton = JButton("Reset Port Overrides").apply {
        toolTipText = "Remove GATEWAY_HTTP_PORT and GATEWAY_HTTPS_PORT env vars so the gateway falls back to the Ignition image defaults (8088/8043)."
        addActionListener { performReset() }
    }

    init {
        add(boldLabel("Docker Port Mappings"), "growx")
        add(portsDisplay, "growx, gapleft 10")

        add(boldLabel("Restore File"), "growx, gaptop 8")
        add(pathField, "growx, gapleft 10")

        add(boldLabel("Gateway Backup Port Settings"), "growx, gaptop 8")
        add(xmlDisplay, "growx, gapleft 10")
        add(resetButton, "gapleft 10")

        model.addServiceModelChangeListener { refresh() }
        refresh()
    }

    override fun updateData() = refresh()

    private fun currentBindMount(): BindMount? {
        val restoreArg = model.commands.firstOrNull { it.startsWith("$restoreFlag ") } ?: return null
        val containerPath = restoreArg.substringAfter("$restoreFlag ").trim()
        return model.volumes.firstOrNull { it.containerPath == containerPath }
    }

    private fun hasPortOverride(): Boolean = httpKey in model.environment || httpsKey in model.environment

    private fun refresh() {
        portsDisplay.text = renderPorts()
        val mount = currentBindMount()
        if (!pathField.isFocusOwner) {
            pathField.text = mount?.bindPath.orEmpty()
        }
        pathField.isEnabled = mount != null
        xmlDisplay.text = renderXml(mount)
        resetButton.isEnabled = hasPortOverride()
    }

    private fun renderPorts(): String = buildString {
        tag("html") {
            if (model.ports.isEmpty()) {
                append("(none)")
            } else {
                for ((i, p) in model.ports.withIndex()) {
                    if (i > 0) append("<br>")
                    append(p.published).append(" → ").append(p.target)
                }
            }
        }
    }

    private fun renderXml(mount: BindMount?): String = buildString {
        tag("html") {
            if (mount == null) {
                append("(no GWBK file linked to this node)")
                return@tag
            }
            val path = Path(mount.bindPath)
            if (!path.exists()) {
                append("(file not found: ").append(path.fileName).append(")")
                return@tag
            }
            runCatching {
                val backup = GatewayBackup(path)
                val http = backup.gatewaySettings.getProperty("gateway.port", "8088")
                val https = backup.gatewaySettings.getProperty("gateway.sslport", "8043")
                val redirect = backup.gatewaySettings.getProperty("gateway.forceSecureRedirect", "false")
                append("HTTP port: ").append(http).append("<br>")
                append("HTTPS port: ").append(https).append("<br>")
                append("Force Secure Redirect: ").append(redirect)
            }.onFailure {
                append("(error reading GWBK: ").append(it.message).append(")")
            }
        }
    }

    private fun commitPath() {
        val mount = currentBindMount() ?: return
        val newValue = pathField.text
        if (newValue == mount.bindPath) return
        mount.bindPath = newValue
        model.fireServiceModelChangedEvent()
        fireConfigChange()
    }

    private fun performReset() {
        if (!hasPortOverride()) return

        val confirm = JOptionPane.showConfirmDialog(
            this,
            "This will remove the GATEWAY_HTTP_PORT and GATEWAY_HTTPS_PORT env vars from this node. " +
                "On startup the Ignition image will force the gateway to default ports (8088/8043), " +
                "overriding the GWBK's port settings.\n\n" +
                "Continue?",
            "Reset Port Overrides",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
        )
        if (confirm != JOptionPane.OK_OPTION) return

        model.environment.remove(httpKey)
        model.environment.remove(httpsKey)
        model.fireServiceModelChangedEvent()
        fireConfigChange()
    }

    private fun boldLabel(text: String) = JLabel(text).apply {
        font = font.deriveFont(Font.BOLD)
    }
}
