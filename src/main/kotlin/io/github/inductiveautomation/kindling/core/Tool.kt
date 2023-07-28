package io.github.inductiveautomation.kindling.core

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.cache.CacheViewer
import io.github.inductiveautomation.kindling.idb.IdbViewer
import io.github.inductiveautomation.kindling.log.LogViewer
import io.github.inductiveautomation.kindling.sim.SimulatorViewer
import io.github.inductiveautomation.kindling.thread.MultiThreadViewer
import io.github.inductiveautomation.kindling.utils.loadService
import io.github.inductiveautomation.kindling.zip.ZipViewer
import java.io.File
import java.nio.file.Path
import javax.swing.filechooser.FileFilter

interface Tool {
    val title: String
    val description: String
    val icon: FlatSVGIcon
    val respectsEncoding: Boolean
        get() = false

    fun open(path: Path): ToolPanel

    val filter: FileFilter

    companion object {
        val tools: List<Tool> by lazy {
            loadService<Tool>().sortedBy { it.title } +
                listOf(
                    ZipViewer,
                    MultiThreadViewer,
                    LogViewer,
                    IdbViewer,
                    CacheViewer,
                    SimulatorViewer,
                )
        }

        val byFilter: Map<FileFilter, Tool> by lazy {
            tools.associateBy(Tool::filter)
        }

        val byTitle: Map<String, Tool> by lazy {
            tools.associateBy(Tool::title)
        }

        fun find(file: File): Tool? = tools.find { tool ->
            tool.filter.accept(file)
        }

        operator fun get(file: File): Tool = checkNotNull(find(file)) { "No tool found for $file" }
    }
}

interface MultiTool : Tool {
    fun open(paths: List<Path>): ToolPanel

    override fun open(path: Path): ToolPanel = open(listOf(path))
}

interface ClipboardTool : Tool {
    fun open(data: String): ToolPanel
}

class ToolOpeningException(message: String, cause: Throwable? = null) : Exception(message, cause)
