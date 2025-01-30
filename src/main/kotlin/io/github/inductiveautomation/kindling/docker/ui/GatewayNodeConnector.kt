package io.github.inductiveautomation.kindling.docker.ui

import io.github.inductiveautomation.kindling.utils.MouseListenerBuilder.Companion.addMouseListener
import io.github.inductiveautomation.kindling.utils.MouseMotionListenerBuilder.Companion.addMouseMotionListener
import net.miginfocom.swing.MigLayout
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.awt.geom.Path2D
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.SwingUtilities

@Suppress("unused")
class GatewayNodeConnector(
    val from: GatewayServiceNode,
    val to: GatewayServiceNode,
    var description: String? = null,
//    var port: UShort,

) : JComponent() {

    init {
        require(from.model.hostName != null && to.model.hostName != null) {
            "Both gateways must have a valid hostname specified."
        }

        layout = null

        addMouseMotionListener {
            mouseMoved {
                val contains = connectionPath.contains(it.point)
                if (contains != highlightPath) {
                    highlightPath = contains
                    repaint()
                }
            }
        }

        addMouseListener {
            mouseExited {
                highlightPath = false
            }
            mouseClicked {
                if (highlightPath && it.clickCount == 2) {
                    val result = JOptionPane.showConfirmDialog(null,
                        "Delete this connection?",
                        "Confirm",
                        JOptionPane.YES_NO_OPTION
                    )

                    if (result == JOptionPane.YES_OPTION) {
                        val parent = SwingUtilities.getUnwrappedParent(this@GatewayNodeConnector)
                        parent.remove(this@GatewayNodeConnector)

                        SwingUtilities.invokeLater {
                            parent.repaint()
                        }
                    }
                }
            }
        }


    }

    private var highlightPath = false

    private var connectionPath = Path2D.Double()

    private val boundsFromConnectionPoints: Rectangle
        get() {
            val minX = minOf(from.x, to.x)
            val minY = minOf(from.y, to.y)
            val maxX = maxOf(from.x + from.width, to.x + to.width)
            val maxY = maxOf(from.y + from.height,to.y + to.height)

            return Rectangle(minX, minY, maxX - minX, maxY - minY)
        }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        if (g !is Graphics2D) return

        if (boundsFromConnectionPoints.width > 0 && boundsFromConnectionPoints.height > 0) {
            bounds = boundsFromConnectionPoints
        }

        g.color = Color.WHITE
        g.stroke = BasicStroke(if (highlightPath) 3F else 1F, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val fromPoint = SwingUtilities.convertPoint(from, from.midPoint(), this)
        val toPoint = SwingUtilities.convertPoint(to, to.midPoint(), this)

        connectionPath.apply {
            reset()
            moveTo(fromPoint.x.toDouble(), fromPoint.y.toDouble())
            curveTo(
                fromPoint.x.toDouble(),
                toPoint.y.toDouble(),
                fromPoint.x.toDouble(),
                toPoint.y.toDouble(),
                toPoint.x.toDouble(),
                toPoint.y.toDouble(),
            )
        }

        g.draw(connectionPath)
    }

    override fun paintBorder(g: Graphics?) {
        super.paintBorder(g)
    }

    companion object {
        fun JComponent.midPoint(): Point {
            return Point(width / 2, height / 2)
        }
    }

    inner class ConnectionSettingsPanel : JPanel(MigLayout("fill, ins 4")) {

    }
}
