package io.github.inductiveautomation.kindling.docker

import io.github.inductiveautomation.kindling.docker.engine.PullProgress
import io.github.inductiveautomation.kindling.utils.toFileSizeLabel
import net.miginfocom.swing.MigLayout
import java.awt.Window
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JScrollPane

/**
 * A small window that shows a labeled progress bar per image while a stack's images are pulled.
 * Rows are created lazily as the first event for each image arrives, so concurrent pulls appear
 * side by side. The owner is responsible for [dispose]-ing it once the pulls finish.
 *
 * All methods must be called on the EDT.
 */
class PullProgressDialog(owner: Window?) : JFrame("Pulling Images") {
    private val content = JPanel(MigLayout("wrap 1, fillx, ins 10, hidemode 3", "[grow, fill]"))

    // Keyed by image reference; insertion order keeps the rows stable as bars are added.
    private val rows = LinkedHashMap<String, Row>()

    private class Row(val label: JLabel, val bar: JProgressBar)

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        contentPane = JScrollPane(content)
        setSize(460, 140)
        setLocationRelativeTo(owner)
    }

    fun update(progress: PullProgress) {
        val row = rows.getOrPut(progress.image) {
            val label = JLabel(progress.image)
            val bar = JProgressBar(0, 100).apply { isStringPainted = true }
            content.add(label)
            content.add(bar)
            content.revalidate()
            Row(label, bar)
        }

        val fraction = progress.fraction
        if (fraction != null) {
            val percent = (fraction * 100).toInt()
            row.bar.isIndeterminate = false
            row.bar.value = percent
            row.bar.string = buildString {
                append(progress.downloadedBytes.toFileSizeLabel())
                append(" / ")
                append(progress.totalBytes.toFileSizeLabel())
                append(" ($percent%)")
            }
        } else {
            row.bar.isIndeterminate = true
            row.bar.string = progress.status.ifEmpty { "Preparing…" }
        }
    }
}
