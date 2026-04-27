package io.github.inductiveautomation.kindling.docker

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.docker.engine.ComposeResult
import io.github.inductiveautomation.kindling.docker.engine.ComposeStatus
import io.github.inductiveautomation.kindling.docker.engine.DockerComposeEngine
import io.github.inductiveautomation.kindling.utils.Action
import io.github.inductiveautomation.kindling.utils.EDT_SCOPE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.nio.file.Path
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

class DockerControlBar(
    val engine: DockerComposeEngine,
    val yamlPath: Path,
    val localHashProvider: () -> String,
) : JPanel(MigLayout("ins 3, fillx, hidemode 3")) {
    private val startAction = Action(
        name = "Start",
        icon = FlatSVGIcon("icons/bx-play.svg"),
    ) {
        runCommand("start") { it.start() }
    }

    private val stopAction = Action(
        name = "Stop",
        icon = FlatSVGIcon("icons/bx-stop.svg"),
    ) {
        runCommand("stop") { it.stop() }
    }

    private val restartAction = Action(
        name = "Restart",
        icon = FlatSVGIcon("icons/bx-refresh.svg"),
    ) {
        runCommand("restart") { it.restart() }
    }

    private val deleteAction = Action(
        name = "Delete",
        icon = FlatSVGIcon("icons/bx-trash.svg"),
    ) {
        val confirm = JOptionPane.showConfirmDialog(
            this@DockerControlBar,
            "This will stop and remove all containers in this compose stack. Proceed?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
        )
        if (confirm == JOptionPane.YES_OPTION) {
            runCommand("delete") { it.delete() }
        }
    }

    private val connectedIndicator = JLabel("•").apply {
        font = font.deriveFont(24f)
        foreground = Color.GRAY
    }

    val startButton = JButton(startAction)
    val stopButton = JButton(stopAction).apply {
        isVisible = false;
    }
    val restartButton = JButton(restartAction)
    val deleteButton = JButton(deleteAction)

    private val buttons = listOf(startButton, stopButton, restartButton, deleteButton)

    init {
        buttons.forEach { add(it) }
        add(connectedIndicator, "push, align right")
        refreshStatus()
    }

    private fun runCommand(label: String, action: (DockerComposeEngine) -> ComposeResult) {
        connectedIndicator.foreground = Color.YELLOW
        buttons.forEach { it.isEnabled = false }
        EDT_SCOPE.launch {
            val result = withContext(Dispatchers.IO) { action(engine) }
            buttons.forEach { it.isEnabled = true }
            refreshStatus()
            if (result is ComposeResult.Failure) {
                showErrorDialog(label, result)
            }
        }
    }

    fun refreshStatus() {
        val status = engine.getStatus()
        when (status) {
            ComposeStatus.Running -> connectedIndicator.foreground = Color.GREEN
            ComposeStatus.Stopped -> connectedIndicator.foreground = Color.RED
            ComposeStatus.Unknown -> connectedIndicator.foreground = Color.GRAY
            is ComposeStatus.Partial -> connectedIndicator.foreground = Color.BLUE
        }
        setButtonVisibility(status)

        if (status is ComposeStatus.Running || status is ComposeStatus.Partial) {
            EDT_SCOPE.launch {
                val remoteHash = withContext(Dispatchers.IO) { engine.getRemoteHash() }
                val localHash = localHashProvider()
                if (remoteHash != localHash) {
                    connectedIndicator.toolTipText = "YAML has changed since containers were started"
                } else {
                    connectedIndicator.toolTipText = "Status: ${status.displayText}"
                }
            }
        }
    }

    private fun setButtonVisibility(status: ComposeStatus) {
        if (status is ComposeStatus.Running || status is ComposeStatus.Partial) {
            startButton.isVisible = false;
            stopButton.isVisible = true;
        }
        else {
            startButton.isVisible = true;
            stopButton.isVisible = false;
        }
    }

    private fun showErrorDialog(command: String, failure: ComposeResult.Failure) {
        val textArea = JTextArea(failure.output).apply {
            isEditable = false
            rows = 12
            columns = 60
        }
        JOptionPane.showMessageDialog(
            this,
            JScrollPane(textArea),
            "Docker Compose $command failed (exit ${failure.exitCode})",
            JOptionPane.ERROR_MESSAGE,
        )
    }
}

private val ComposeStatus.displayText: String
    get() = when (this) {
        ComposeStatus.Running -> "Status: Running"
        ComposeStatus.Stopped -> "Status: Stopped"
        is ComposeStatus.Partial -> "Status: Partial ($running/$total)"
        ComposeStatus.Unknown -> "Status: Unknown"
    }
