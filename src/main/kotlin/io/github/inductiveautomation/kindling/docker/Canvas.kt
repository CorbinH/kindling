package io.github.inductiveautomation.kindling.docker

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.docker.services.AbstractDockerServiceNode
import io.github.inductiveautomation.kindling.docker.services.ignition.IgnitionNodeConnector
import io.github.inductiveautomation.kindling.utils.MouseListenerBuilder.Companion.addMouseListener
import io.github.inductiveautomation.kindling.utils.MouseMotionListenerBuilder.Companion.addMouseMotionListener
import io.github.inductiveautomation.kindling.utils.traverseChildren
import java.awt.Component
import java.awt.EventQueue
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.MouseInfo
import java.awt.Point
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities
import kotlin.math.ceil
import kotlin.math.sqrt

class Canvas(
    private val label: String,
) : JLayeredPane() {
    private lateinit var canvasDragPoint: Point

    init {
        isOpaque = true
        layout = null

        addMouseListener {
            mousePressed {
                canvasDragPoint = it.point
            }
        }

        addMouseMotionListener {
            mouseDragged { e ->
                val deltaX = e.x - canvasDragPoint.x
                val deltaY = e.y - canvasDragPoint.y

                traverseChildren(false).forEach { c ->
                    if (c !== arrangeButton) {
                        c.setLocation(c.x + deltaX, c.y + deltaY)
                    }
                }

                canvasDragPoint = e.point
            }
            mouseMoved { e ->
                val allConnections = traverseChildren(false).filterIsInstance<IgnitionNodeConnector>().toList()
                allConnections.forEach { connection ->
                    val connectionPoint = SwingUtilities.convertPoint(this@Canvas, e.point, connection)
                    if (connection.contains(connectionPoint)) {
                        this@Canvas.setLayer(connection, CONNECTION_LAYER, 0)
                    }
                }
            }
        }

        SwingUtilities.invokeLater {
            add(arrangeButton)
        }
    }

    private val arrangeButton = JButton(FlatSVGIcon("icons/bxs-grid-alt.svg")).apply {
        toolTipText = "Tile-arrange"
        addActionListener { tileArrange() }
    }

    private fun tileArrange() {
        val nodes = traverseChildren(false).filterIsInstance<AbstractDockerServiceNode<*>>().toList()

        if (nodes.isEmpty()) return

        val cols = ceil(sqrt(nodes.size.toDouble())).toInt()
        val rows = Math.ceilDiv(nodes.size, cols)
        val lastRow = nodes.size - (rows - 1) * cols

        var n = 0
        val partitionWidth = width / cols
        val partitionHeight = height / rows

        for (r in 0..<rows - 1) {
            for (c in 0..<cols) {
                val locationX = partitionWidth * (c + 0.5) - nodes[n].width / 2
                val locationY = partitionHeight * (r + 0.5) - nodes[n].height / 2
                nodes[n].location = Point(locationX.toInt(), locationY.toInt())
                n++
            }
        }

        for (c in 0..<lastRow) {
            val locationX = (width / lastRow) * (c + 0.5) - nodes[n].width / 2
            val locationY = partitionHeight * (rows - 0.5) - nodes[n].height / 2
            nodes[n].location = Point(locationX.toInt(), locationY.toInt())
            n++
        }
    }

    override fun addImpl(comp: Component?, constraints: Any?, index: Int) {
        super.addImpl(comp, constraints, index)
        if (comp is AbstractDockerServiceNode<*>) {
            attachDragListeners(comp)
            comp.addNodeDeleteListener {
                remove(comp)
            }
            EventQueue.invokeLater {
                // If location is specified, place the component there, otherwise place it in the center.
                val (x, y) = if (constraints is Point) {
                    listOf(constraints.x, constraints.y)
                } else {
                    listOf(
                        (width / 2) - (comp.preferredSize.width / 2),
                        (height / 2) - (comp.preferredSize.width / 2),
                    )
                }

                comp.setBounds(x, y, comp.preferredSize.width, comp.preferredSize.height)
                setLayer(comp, NODE_LAYER, 0)
                comp.revalidate()
                comp.repaint()
            }
        }
    }

    private fun attachDragListeners(c: JComponent) {
        // The point on the canvas when the mouse is clicked and dragged. Tracked per component.
        var cPoint: Point? = null

        c.addMouseListener {
            mousePressed {
                cPoint = SwingUtilities.convertPoint(c, it.x, it.y, this@Canvas)
                this@Canvas.setLayer(c, NODE_LAYER, 0)
                c.repaint()
            }
        }

        c.addMouseMotionListener {
            mouseDragged {
                val newEventPoint = SwingUtilities.convertPoint(c, it.x, it.y, this@Canvas)

                val deltaX = newEventPoint.x - (cPoint?.x ?: 0)
                val deltaY = newEventPoint.y - (cPoint?.y ?: 0)

                val newComponentX = (c.x + deltaX).coerceIn(0..width - c.width)
                val newComponentY = (c.y + deltaY).coerceIn(0..height - c.height)

                c.setLocation(newComponentX, newComponentY)

                @Suppress("AssignedValueIsNeverRead")
                cPoint = newEventPoint
            }
        }
    }

    init {
        addMouseMotionListener {
            mouseMoved {
                mouseLoc = it.point
                repaint()
            }
        }

        addMouseListener {
            mouseClicked {

            }
        }
    }

    private var mouseLoc: Point = MouseInfo.getPointerInfo().location.also {
        SwingUtilities.convertPointFromScreen(it, this)
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)

        if (g !is Graphics2D) return

        g.font = labelFont
        g.drawString(label, 10, labelFont.size + 10)
        arrangeButton.setBounds(width - 40, 10, 30, 30)
    }

    companion object {
        private val labelFont: Font = Font(Font.SANS_SERIF, Font.BOLD, 24)

        const val NODE_LAYER = 2
        const val CONNECTION_LAYER = 1
    }
}