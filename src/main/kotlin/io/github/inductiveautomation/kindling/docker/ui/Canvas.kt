package io.github.inductiveautomation.kindling.docker.ui

import io.github.inductiveautomation.kindling.utils.MouseListenerBuilder.Companion.addMouseListener
import io.github.inductiveautomation.kindling.utils.MouseMotionListenerBuilder.Companion.addMouseMotionListener
import io.github.inductiveautomation.kindling.utils.traverseChildren
import java.awt.Component
import java.awt.EventQueue
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.util.EventListener
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.TransferHandler
import javax.swing.event.EventListenerList

class Canvas(
    private val label: String,
) : JPanel() {
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
                    c.setLocation(c.x + deltaX, c.y + deltaY)
                }

                canvasDragPoint = e.point
            }
        }

        transferHandler = object : TransferHandler() {
            override fun canImport(support: TransferSupport?): Boolean {
                return support?.isDataFlavorSupported(NodeInitializerTransferHandler.NODE_INITIALIZER_DATA_FLAVOR) ?: false
            }

            override fun importData(support: TransferSupport?): Boolean {
                if (!canImport(support)) return false

                val initializer = support?.transferable?.getTransferData(NodeInitializerTransferHandler.NODE_INITIALIZER_DATA_FLAVOR)

                if (initializer is NodeInitializer) {
                    val canvas = support.component as? Canvas ?: return false
                    val node = initializer.initialize()

                    val dropLocation = support.dropLocation.dropPoint.let {
                        Point(it.x - node.preferredSize.width / 2, it.y - node.preferredSize.height / 2)
                    }

                    canvas.add(node, dropLocation)
                    return true
                }

                return false
            }
        }
    }

    override fun addImpl(comp: Component?, constraints: Any?, index: Int) {
        if (comp !is JComponent) return
        super.addImpl(comp, constraints, index)
        attachDragListeners(comp)

        EventQueue.invokeLater {
            // If location is specified, place the component there, otherwise place it in the center.
            val (x, y) = if (constraints is Point) {
                listOf(constraints.x, constraints.y)
            } else {
                listOf(
                    (width / 2) - (comp.preferredSize.width / 2),
                    (height / 2) - (comp.preferredSize.width / 2)
                )
            }

            comp.setBounds(x, y, comp.preferredSize.width, comp.preferredSize.height)
        }
    }

    private fun attachDragListeners(c: JComponent) {
        // The point on the canvas when the mouse is clicked and dragged. Tracked per component.
        var cPoint: Point? = null

        c.addMouseListener {
            mousePressed {
                cPoint = SwingUtilities.convertPoint(c, it.x, it.y, this@Canvas)
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
                cPoint = newEventPoint
                this@Canvas.repaint()
            }
        }
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)

        if (g !is Graphics2D) return

        g.font = labelFont
        g.drawString(label, 10, labelFont.size + 10)

        paintLinks(g)
    }

    private fun paintLinks(g: Graphics2D) {
        val children = traverseChildren(false).filterIsInstance<CanvasLinkable>()
        for (child in children) {
            child as Component
            child.outgoingLink?.let {
                if (it !is Component) return@let
                g.drawLine(
                    child.x + child.width / 2,
                    child.y + child.height / 2,
                    it.x + it.width / 2,
                    it.y + it.height / 2,
                )
            }
        }
    }

    companion object {
        private val labelFont: Font = Font(Font.SANS_SERIF, Font.BOLD, 24)
    }
}

interface CanvasLinkable {
    val linkListeners: EventListenerList
    var incomingLink: CanvasLinkable?
    var outgoingLink: CanvasLinkable?
    fun fireLinkStartEvent()

    companion object {
        fun CanvasLinkable.linkComponent(c: CanvasLinkable) {
            outgoingLink = c
            c.incomingLink = this
        }
    }
}

fun interface CanvasLinkBeginListener : EventListener {
    fun onLinkStart()
}
