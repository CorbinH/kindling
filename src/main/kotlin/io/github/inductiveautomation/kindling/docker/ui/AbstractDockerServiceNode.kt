package io.github.inductiveautomation.kindling.docker.ui

import io.github.inductiveautomation.kindling.docker.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.model.DockerServiceModel
import io.github.inductiveautomation.kindling.docker.model.DockerVolume
import io.github.inductiveautomation.kindling.utils.jFrame
import java.awt.Color
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import net.miginfocom.swing.MigLayout

@Suppress("LeakingThis")
abstract class AbstractDockerServiceNode<T : DockerServiceModel> : JPanel(MigLayout("fill, ins 4")) {
    abstract val model: T

    abstract var volumeOptions: Set<DockerVolume>

    abstract var networks: Set<DockerNetwork>

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

    init {
        isOpaque = true
        border = BorderFactory.createLineBorder(Color.GRAY, 1, true)

        add(serviceNameLabel, "growx, spanx")
        add(hostNameLabel, "growx, spanx")
        add(configureButton, "growx, spanx")
    }
}

abstract class NodeConfigPanel(constraints: String) : JPanel(MigLayout(constraints)) {
    protected abstract val generalSection: JPanel
    protected abstract val portsSection: JPanel
    protected abstract val envSection: JPanel
    protected abstract val cliSection: JPanel
    protected abstract val volumesSection: JPanel
    protected abstract val networksSection: JPanel

    companion object {
        fun configSection(title: String, constraints: String = "fill, ins 0", block: JPanel.() -> Unit): JPanel {
            return JPanel(MigLayout(constraints)).apply {
                border = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), title).apply {
                    this.titleFont = titleFont.deriveFont(Font.BOLD, 14F)
                }
                block()
            }
        }
    }
}

open class ConfigSection(
    title: String,
    constraints: String = "fill, ins 0",
) : JPanel(MigLayout(constraints)) {
    init {
        border = BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), title).apply {
            this.titleFont = titleFont.deriveFont(Font.BOLD, 14F)
        }
    }

//    final override fun getBorder(): Border {
//        return super.getBorder()
//    }
//
//    final override fun setBorder(border: Border?) {
//        super.setBorder(border)
//    }
}

