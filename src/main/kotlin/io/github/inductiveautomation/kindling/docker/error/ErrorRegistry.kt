package io.github.inductiveautomation.kindling.docker.error

import io.github.inductiveautomation.kindling.docker.services.AbstractDockerServiceNode
import io.github.inductiveautomation.kindling.utils.add
import io.github.inductiveautomation.kindling.utils.getAll
import io.github.inductiveautomation.kindling.utils.remove
import java.nio.file.Path
import java.util.EventListener
import javax.swing.event.EventListenerList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext

class ErrorRegistry(
    private val nodesProvider: () -> List<AbstractDockerServiceNode<*>>,
    private val baseDirProvider: () -> Path?,
) {
    var errors: List<DockerError> = emptyList()
        private set

    private val listenerList = EventListenerList()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentJob: Job? = null

    fun addErrorsChangedListener(l: ErrorsChangedListener) = listenerList.add(l)
    fun removeErrorsChangedListener(l: ErrorsChangedListener) = listenerList.remove(l)

    fun refresh() {
        val nodes = nodesProvider()
        val baseDir = baseDirProvider()

        currentJob?.cancel()
        currentJob = scope.launch {
            val results = DockerErrorChecker.all
                .map { checker -> async { runCatching { checker.check(nodes, baseDir) }.getOrElse { emptyList() } } }
                .awaitAll()
                .flatten()

            withContext(Dispatchers.Swing) {
                errors = results
                listenerList.getAll<ErrorsChangedListener>().forEach { it.onErrorsChanged(results) }
            }
        }
    }
}

fun interface ErrorsChangedListener : EventListener {
    fun onErrorsChanged(errors: List<DockerError>)
}
