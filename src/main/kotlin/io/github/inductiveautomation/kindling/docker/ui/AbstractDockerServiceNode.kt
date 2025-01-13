package io.github.inductiveautomation.kindling.docker.ui

import io.github.inductiveautomation.kindling.docker.model.DockerServiceModel
import io.github.inductiveautomation.kindling.utils.jFrame
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import net.miginfocom.swing.MigLayout

abstract class AbstractDockerServiceNode<T : DockerServiceModel> : JPanel(MigLayout("fill, ins 4")) {
    abstract val model: T

    abstract val configEditor: JComponent

    abstract val header: JComponent

    protected val serviceNameLabel = JLabel()
    protected val hostNameLabel = JLabel()
    private val configureButton = JButton("Configure").apply {
        addActionListener {
            jFrame("Edit Docker Config", 500, 400) {
                add(configEditor)
                pack()
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