package io.github.inductiveautomation.kindling.docker.ui

import io.github.inductiveautomation.kindling.docker.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.model.DockerServiceModel
import io.github.inductiveautomation.kindling.docker.model.DockerVolume
import io.github.inductiveautomation.kindling.docker.model.ServiceModelChangeListener
import io.github.inductiveautomation.kindling.utils.add
import io.github.inductiveautomation.kindling.utils.getAll
import io.github.inductiveautomation.kindling.utils.jFrame
import java.awt.Color
import java.awt.Font
import java.util.EventListener
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import net.miginfocom.swing.MigLayout

@Suppress("LeakingThis")
abstract class AbstractDockerServiceNode<T : DockerServiceModel> : JPanel(MigLayout("fill, ins 4")) {
    abstract val model: T

    abstract var volumeOptions: List<DockerVolume>

    abstract var networkOptions: List<DockerNetwork>

    abstract val configEditor: NodeConfigPanel

    abstract val header: JComponent

    protected val serviceNameLabel = JLabel()
    protected val hostNameLabel = JLabel()
    private val configureButton = JButton("Configure").apply {
        addActionListener {
            jFrame("Edit Docker Config", 1000, 600) {
                contentPane = configEditor
            }
        }
    }

    fun addServiceModelChangeListener(l: ServiceModelChangeListener) {
        listenerList.add(l)
    }

    fun fireServiceModelChangedEvent() {
        listenerList.getAll<ServiceModelChangeListener>().forEach(ServiceModelChangeListener::onServiceModelChanged)
    }

    fun addNodeDeleteListener(l: NodeDeleteListener<AbstractDockerServiceNode<T>>) = listenerList.add(l)

    protected fun fireNodeDeletedEvent() {
        listenerList.getAll<NodeDeleteListener<AbstractDockerServiceNode<T>>>().forEach {
            it.onNodeDelete(this)
        }
    }

    init {
        isOpaque = true
        border = BorderFactory.createLineBorder(Color.GRAY, 1, true)

        add(serviceNameLabel, "growx, spanx")
        add(hostNameLabel, "growx, spanx")
        add(configureButton, "growx, spanx")
    }
}

fun interface NodeDeleteListener<T : AbstractDockerServiceNode<out DockerServiceModel>> : EventListener {
    fun onNodeDelete(node: T)
}

abstract class NodeConfigPanel(constraints: String) : JPanel(MigLayout(constraints)) {
    protected abstract val node: AbstractDockerServiceNode<out DockerServiceModel>

    protected abstract val generalSection: ConfigSection
    protected abstract val portsSection: ConfigSection
    protected abstract val envSection: ConfigSection
    protected abstract val cliSection: ConfigSection
    protected abstract val volumesSection: ConfigSection
    protected abstract val networksSection: ConfigSection

    protected fun <T : ConfigSection> T.bind(): T {
        addChangeListener {
            node.fireServiceModelChangedEvent()
        }
        return this
    }
}

abstract class ConfigSection(
    title: String,
    constraints: String = "fill, ins 0",
) : JPanel(MigLayout(constraints)) {
    init {
        border = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), title).apply {
            this.titleFont = titleFont.deriveFont(Font.BOLD, 14F)
        }
    }

    open fun updateData() = Unit
    fun addChangeListener(l: ConfigChangeListener) {
        listenerList.add(l)
    }

    protected fun fireConfigChange() {
        listenerList.getAll<ConfigChangeListener>().forEach(ConfigChangeListener::onConfigChange)
    }

    fun interface ConfigChangeListener : EventListener {
        fun onConfigChange()
    }
}
