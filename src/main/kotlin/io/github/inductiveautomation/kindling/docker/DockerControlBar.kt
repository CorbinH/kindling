package io.github.inductiveautomation.kindling.docker

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.core.StackShutdownBehavior
import io.github.inductiveautomation.kindling.docker.engine.ComposeResult
import io.github.inductiveautomation.kindling.docker.engine.ComposeStatus
import io.github.inductiveautomation.kindling.docker.engine.ContainerStats
import io.github.inductiveautomation.kindling.docker.engine.DockerComposeEngine
import io.github.inductiveautomation.kindling.docker.engine.PullProgress
import io.github.inductiveautomation.kindling.docker.engine.StackSnapshot
import io.github.inductiveautomation.kindling.utils.Action
import io.github.inductiveautomation.kindling.utils.EDT_SCOPE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import javax.swing.Timer

class DockerControlBar(
    val engine: DockerComposeEngine,
    val yamlPath: Path,
    val localHashProvider: () -> String,
    private val onStatusChange: (ComposeStatus) -> Unit = {},
    private val onStats: (Map<String, ContainerStats>) -> Unit = {},
) : JPanel(MigLayout("ins 3, fillx, hidemode 3")) {

    // Actions are inlined into their buttons since each is only used in one place
    val startButton = JButton(
        Action(name = "Start", icon = FlatSVGIcon("icons/bx-play.svg")) {
            runCommand("start") { it.start(::onPullProgress) }
        },
    )

    val stopButton = JButton(
        Action(name = "Stop", icon = FlatSVGIcon("icons/bx-stop.svg")) {
            runCommand("stop") { it.stop() }
        },
    ).apply {
        isEnabled = false
    }

    val restartButton = JButton(
        Action(name = "Restart", icon = FlatSVGIcon("icons/bx-refresh.svg")) {
            runCommand("restart") { it.restart() }
        },
    )

    val rebuildButton = JButton(
        Action(name = "Rebuild", icon = FlatSVGIcon("icons/bx-reload.svg")) {
            // Both steps must run in one command so start waits for delete to finish; two separate
            // runCommand calls would launch concurrent coroutines that race on the IO dispatcher.
            runCommand("rebuild") { engine ->
                when (val deleteResult = engine.delete()) {
                    is ComposeResult.Failure -> deleteResult
                    else -> engine.start(::onPullProgress)
                }
            }
        },
    )

    val deleteButton = JButton(
        Action(name = "Delete", icon = FlatSVGIcon("icons/bx-trash.svg")) {
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
        },
    )

    // Colored dot ("●") plus the state text; the whole label is tinted to the state color.
    private val statusIndicator = JLabel()

    private val buttons = listOf(startButton, stopButton, restartButton, deleteButton, rebuildButton)

    // Created lazily on the first pull event and disposed when the command finishes.
    private var pullDialog: PullProgressDialog? = null

    // Last reported percent per image, so we only touch the EDT when the percent actually changes.
    // Pull callbacks fire from several docker-java threads at once, so this must be thread-safe.
    private val lastPercentByImage = ConcurrentHashMap<String, Int>()

    // Cached result of the last Docker poll. The stale/in-sync check re-renders off this without a
    // new Docker call when the model changes (see onModelChanged).
    private var lastSnapshot = StackSnapshot(ComposeStatus.Unknown, null)

    // Re-polls Docker so status set outside the app (container crash, `docker stop`) is reflected.
    private val pollTimer = Timer(POLL_INTERVAL_MS) {
        // Skip a tick if the previous poll is still in flight rather than stacking calls.
        if (pollJob?.isActive != true) {
            pollJob = EDT_SCOPE.launch { refreshStatus() }
        }
    }
    private var pollJob: Job? = null

    // Faster, separate poll for per-container resource stats; only queries Docker while running.
    private val statsTimer = Timer(STATS_INTERVAL_MS) {
        val running = lastSnapshot.status is ComposeStatus.Running || lastSnapshot.status is ComposeStatus.Partial
        if (running && statsJob?.isActive != true) {
            statsJob = EDT_SCOPE.launch {
                val stats = withContext(Dispatchers.IO) { engine.getStats() }
                onStats(stats)
            }
        }
    }
    private var statsJob: Job? = null

    init {
        val buttonContainer = JPanel(MigLayout("filly, ins 0"))
        buttons.forEach {
            buttonContainer.add(it, "growy")
        }
        val spacer = JPanel().apply {
            isVisible = false
        }

        add(buttonContainer, "growx, sg")
        add(statusIndicator, "growx, sg")
        add(spacer, "growx, sg")

        renderIndicator()
        // Runs the first status check off the EDT so startup doesn't block the UI, then polls.
        EDT_SCOPE.launch { refreshStatus() }
        pollTimer.start()
        statsTimer.start()
    }

    override fun removeNotify() {
        // Stop polling when the tab is closed so the timers/coroutines don't leak.
        pollTimer.stop()
        statsTimer.stop()
        pollJob?.cancel()
        statsJob?.cancel()
        super.removeNotify()
    }

    private fun runCommand(label: String, action: (DockerComposeEngine) -> ComposeResult) {
        // Pause polling so a mid-command tick doesn't briefly overwrite the "Working" indicator.
        pollTimer.stop()
        statsTimer.stop()
        statusIndicator.text = "$DOT Working…"
        statusIndicator.foreground = Color.YELLOW
        statusIndicator.toolTipText = null
        buttons.forEach { it.isEnabled = false }
        EDT_SCOPE.launch {
            val result = withContext(Dispatchers.IO) { action(engine) }
            disposePullDialog()
            buttons.forEach { it.isEnabled = true }
            refreshStatus()
            pollTimer.start()
            statsTimer.start()
            if (result is ComposeResult.Failure) {
                showErrorDialog(label, result)
            }
        }
    }

    // Called off the EDT from the engine's pull callback; marshals updates onto the EDT.
    private fun onPullProgress(progress: PullProgress) {
        val percent = progress.fraction?.let { (it * 100).toInt() } ?: -1
        // Skip redundant updates: same percent (or still indeterminate, -1) as last time.
        if (lastPercentByImage.put(progress.image, percent) == percent) return
        SwingUtilities.invokeLater {
            val dialog = pullDialog ?: PullProgressDialog(SwingUtilities.getWindowAncestor(this)).also {
                pullDialog = it
                it.isVisible = true
            }
            dialog.update(progress)
        }
    }

    private fun disposePullDialog() {
        lastPercentByImage.clear()
        SwingUtilities.invokeLater {
            pullDialog?.dispose()
            pullDialog = null
        }
    }

    // Suspend so the blocking Docker query runs off the EDT; the render step then runs on the EDT.
    private suspend fun refreshStatus() {
        val snapshot = withContext(Dispatchers.IO) { engine.getSnapshot() }
        lastSnapshot = snapshot
        setButtonEnabledState(snapshot.status)
        renderIndicator()
        onStatusChange(snapshot.status)
    }

    /**
     * Re-renders the indicator from the last polled snapshot and the current local hash, without
     * touching Docker. Call this on every model change so the In Sync -> Stale transition shows up
     * live as the YAML is edited.
     */
    fun onModelChanged() = renderIndicator()

    private fun renderIndicator() {
        val rendered = lastSnapshot.render(localHashProvider())
        statusIndicator.text = "$DOT ${rendered.text}"
        statusIndicator.foreground = rendered.color
        statusIndicator.toolTipText = rendered.tooltip
    }

    private fun setButtonEnabledState(status: ComposeStatus) {
        // Start only when there's a stopped or nonexistent stack to bring up; every other action
        // operates on a live stack, so it's only enabled while containers are running.
        val running = status is ComposeStatus.Running || status is ComposeStatus.Partial
        startButton.isEnabled = status is ComposeStatus.Stopped || status is ComposeStatus.NoStack
        (buttons - startButton).forEach { it.isEnabled = running }
    }

    /**
     * Applies the configured shutdown behavior to this stack. Blocking; intended to be called on
     * application close, so status is read fresh rather than from the cached poll.
     */
    fun applyShutdownBehavior(behavior: StackShutdownBehavior) {
        if (behavior == StackShutdownBehavior.LeaveRunning) return
        when (val status = engine.getSnapshot().status) {
            ComposeStatus.NoStack, ComposeStatus.Unknown -> Unit // nothing to act on
            else -> when (behavior) {
                StackShutdownBehavior.LeaveRunning -> Unit
                StackShutdownBehavior.Stop ->
                    if (status is ComposeStatus.Running || status is ComposeStatus.Partial) engine.stop()
                StackShutdownBehavior.Delete -> engine.delete()
            }
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

    private data class RenderedStatus(val text: String, val color: Color, val tooltip: String)

    private fun StackSnapshot.render(localHash: String): RenderedStatus {
        // A stack is stale when its containers carry a hash that no longer matches the editor.
        val stale = remoteHash != null && remoteHash != localHash
        return when (status) {
            ComposeStatus.Unknown -> RenderedStatus(
                "Docker Unavailable",
                Color.GRAY,
                "The Docker engine could not be reached",
            )
            ComposeStatus.NoStack -> RenderedStatus(
                "No Stack",
                Color.GRAY,
                "No containers exist for this stack",
            )
            ComposeStatus.Stopped -> RenderedStatus(
                "Stopped",
                Color.RED,
                "Containers exist but are not running",
            )
            ComposeStatus.Running -> if (stale) {
                RenderedStatus("Running, Stale", Color.ORANGE, "YAML has changed since the stack was started")
            } else {
                RenderedStatus("Running, In Sync", Color.GREEN, "Stack is running and matches the current YAML")
            }
            is ComposeStatus.Partial -> {
                val counts = "${status.running}/${status.total}"
                if (stale) {
                    RenderedStatus("Partial ($counts), Stale", Color.ORANGE, "Some containers are running; YAML has changed since start")
                } else {
                    RenderedStatus("Partial ($counts)", Color.BLUE, "Some containers are running")
                }
            }
        }
    }

    companion object {
        private const val DOT = "●"
        private const val POLL_INTERVAL_MS = 3000
        private const val STATS_INTERVAL_MS = 1000
    }
}
