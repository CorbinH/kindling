package io.github.inductiveautomation.kindling.docker.services

import io.github.inductiveautomation.kindling.docker.services.model.DockerServiceModel
import io.github.inductiveautomation.kindling.docker.volumes.model.DockerVolume
import io.github.inductiveautomation.kindling.utils.EDT_SCOPE
import io.github.inductiveautomation.kindling.utils.FloatableComponent
import io.github.inductiveautomation.kindling.utils.TabStrip
import io.github.inductiveautomation.kindling.utils.add
import io.github.inductiveautomation.kindling.utils.debounce
import io.github.inductiveautomation.kindling.utils.getAll
import io.github.inductiveautomation.kindling.utils.jFrame
import io.github.inductiveautomation.kindling.utils.remove
import io.github.inductiveautomation.kindling.utils.tag
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.awt.Font
import java.awt.Frame
import java.awt.Point
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.EventListener
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.time.Duration.Companion.milliseconds

@Suppress("LeakingThis")
abstract class AbstractDockerServiceNode<T : DockerServiceModel>(
    val model: T,
) : JPanel(MigLayout("fill, ins 4, hidemode 3")) {
    abstract var volumeOptions: List<DockerVolume>

    abstract var networkOptions: List<String>

    abstract val configEditor: NodeConfigPanel

    abstract val header: JComponent

    private val serviceNameLabel = object : JLabel() {
        var internalText: String = text
            set(value) {
                field = value
                text = buildString {
                    tag("html") {
                        tag("b") {
                            append("Name: ")
                        }
                        append(model.containerName)
                    }
                }
            }
    }

    private val hostNameLabel = object : JLabel() {
        var internalText: String? = text
            set(value) {
                field = value
                text = buildString {
                    tag("html") {
                        tag("b") {
                            append("Hostname: ")
                        }
                        if (model.hostName.isNullOrEmpty()) {
                            tag("i") {
                                append("(default)")
                            }
                        } else {
                            append(model.hostName)
                        }
                    }
                }
            }
    }

    protected val configWindow by lazy {
        jFrame("Edit Docker Config", 1000, 600) {
            contentPane = configEditor
        }
    }

    protected val configureButton = JButton("Configure").apply {
        addActionListener {
            openConfigWindow()
        }
    }

    init {
        isOpaque = true
        border = BorderFactory.createLineBorder(Color.GRAY, 1, true)

        add(serviceNameLabel, "growx, spanx")
        add(hostNameLabel, "growx, spanx")
        add(configureButton, "growx, spanx")

        addComponentListener(
            object : ComponentAdapter() {
                override fun componentMoved(e: ComponentEvent?) {
                    updateLocationInfo()
                }
            },
        )

        model.addServiceModelChangeListener {
            if (model.hostName != hostNameLabel.internalText) {
                updateHostNameText()
                SwingUtilities.invokeLater {
                    setBounds(x, y, preferredSize.width, preferredSize.height)
                    revalidate()
                    repaint()
                }
            }

            if (model.containerName != serviceNameLabel.internalText) {
                updateContainerNameText()
                SwingUtilities.invokeLater {
                    setBounds(x, y, preferredSize.width, preferredSize.height)
                    revalidate()
                    repaint()
                }
            }
        }
    }

    fun openConfigWindow() {
        if (configWindow.state == Frame.ICONIFIED) {
            configWindow.state = Frame.NORMAL
        }
        configWindow.isVisible = true
    }

    protected fun updateHostNameText() {
        hostNameLabel.internalText = model.hostName
    }

    protected fun updateContainerNameText() {
        serviceNameLabel.internalText = model.containerName
    }

    fun addNodeDeleteListener(l: NodeDeleteListener) = listenerList.add(l)

    @Suppress("unused")
    fun removeNodeDeleteListener(l: NodeDeleteListener) = listenerList.remove(l)

    protected fun fireNodeDeletedEvent() {
        listenerList.getAll<NodeDeleteListener>().forEach {
            it.onNodeDelete()
        }
    }

    private val updateLocationInfo: () -> Unit = debounce(300.milliseconds, EDT_SCOPE) {
        model.canvasLocation = Point(x, y)
        model.fireServiceModelChangedEvent()
    }
}

fun interface NodeDeleteListener : EventListener {
    fun onNodeDelete()
}

abstract class NodeConfigPanel : TabStrip() {
    protected abstract val node: AbstractDockerServiceNode<out DockerServiceModel>

    protected abstract val generalSection: ConfigSection
    protected abstract val portsSection: ConfigSection
    protected abstract val envSection: ConfigSection
    protected abstract val cliSection: ConfigSection
    protected abstract val volumesSection: ConfigSection
    protected abstract val networksSection: ConfigSection
    protected abstract val genericSection: ConfigSection

    protected fun <T : ConfigSection> T.bind(): T {
        addChangeListener {
            node.model.fireServiceModelChangedEvent()
        }
        return this
    }

    init {
        isTabsClosable = false
    }
}

abstract class ConfigSection(
    title: String,
    constraints: String = "fill, ins 0",
) : JPanel(MigLayout(constraints)),
    FloatableComponent {
    override val icon: Icon? = null
    override val tabTooltip: String? = null
    override val tabName: String = title

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
