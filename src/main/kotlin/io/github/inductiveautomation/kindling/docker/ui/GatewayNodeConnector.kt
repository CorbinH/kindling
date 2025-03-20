package io.github.inductiveautomation.kindling.docker.ui


import io.github.inductiveautomation.kindling.docker.model.ConnectionDefinition
import io.github.inductiveautomation.kindling.docker.model.GatewayEnvironmentVariableDefinition.Companion.addOrRemove
import io.github.inductiveautomation.kindling.docker.model.GatewayEnvironmentVariableDefinition.Companion.createConnectionVariable
import io.github.inductiveautomation.kindling.docker.model.IgnitionVersionComparator
import io.github.inductiveautomation.kindling.docker.model.ServiceModelChangeListener
import io.github.inductiveautomation.kindling.utils.Action
import io.github.inductiveautomation.kindling.utils.MouseListenerBuilder.Companion.addMouseListener
import io.github.inductiveautomation.kindling.utils.MouseMotionListenerBuilder.Companion.addMouseMotionListener
import io.github.inductiveautomation.kindling.utils.PointHelpers.component1
import io.github.inductiveautomation.kindling.utils.PointHelpers.component2
import io.github.inductiveautomation.kindling.utils.PointHelpers.convert
import io.github.inductiveautomation.kindling.utils.attachPopupMenu
import io.github.inductiveautomation.kindling.utils.jFrame
import io.github.inductiveautomation.kindling.utils.tag
import net.miginfocom.swing.MigLayout
import java.awt.BasicStroke
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Polygon
import java.awt.Rectangle
import java.awt.Shape
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D.OUT_BOTTOM
import java.awt.geom.Rectangle2D.OUT_LEFT
import java.awt.geom.Rectangle2D.OUT_RIGHT
import java.awt.geom.Rectangle2D.OUT_TOP
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JSpinner
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel
import javax.swing.SwingUtilities
import javax.swing.SwingUtilities.getUnwrappedParent
import javax.swing.Timer

/**
 * Responsible for drawing connection on the screen and editing connection properties.
 */
class GatewayNodeConnector(
    fromGateway: GatewayServiceNode,
    val index: Int,
    private val canvas: Canvas,
) : JComponent() {
    var from: GatewayServiceNode = fromGateway
    lateinit var to: GatewayServiceNode

    private val connectionInProgress: Boolean
        get() = !::to.isInitialized

    private var mouseLocation: Point = MouseInfo.getPointerInfo().location

    private val mouseObserver = Timer(10) {
        if (!connectionInProgress) {
            SwingUtilities.invokeLater {
                this@GatewayNodeConnector.finalizeConnection()
            }
            return@Timer
        }
        val mouseLoc = MouseInfo.getPointerInfo().location
        if (mouseLocation != mouseLoc) {
            mouseLocation = mouseLoc
            repaint()
        }
    }

    private var highlightPath = false
    private var connectionPath = Path2D.Double()
    private var boundingPath = Path2D.Double()
    private val curveWidth = 4

    private val settingsPanel get() = ConnectionSettingsPanel()

    private val boundsFromConnectionPoints: Rectangle
        get() {
            if (connectionInProgress) {
                val p: Point = mouseLocation.clone() as Point
                SwingUtilities.convertPointFromScreen(p, canvas)

                val minX = minOf(from.x, p.x)
                val minY = minOf(from.y, p.y)
                val maxX = maxOf(from.x + from.width, p.x)
                val maxY = maxOf(from.y + from.height, p.y)

                return Rectangle(minX, minY, maxX - minX + 5, maxY - minY + 5)
            } else {
                val minX = minOf(from.x, to.x)
                val minY = minOf(from.y, to.y)
                val maxX = maxOf(from.x + from.width, to.x + to.width)
                val maxY = maxOf(from.y + from.height, to.y + to.height)

                return Rectangle(minX, minY, maxX - minX, maxY - minY)
            }
        }

    override fun contains(point: Point): Boolean {
        return boundingPath.contains(point)
    }

    init {
        layout = null
        bounds = boundsFromConnectionPoints
        toolTipText = "Right-click to edit/delete"
        addMouseMotionListener {
            mouseMoved {
                val contains = boundingPath.contains(it.point)
                if (contains != highlightPath) {
                    highlightPath = contains
                    repaint()
                } else if (!connectionInProgress) {
                    val parentEvent: MouseEvent = SwingUtilities.convertMouseEvent(
                        this@GatewayNodeConnector,
                        it,
                        this@GatewayNodeConnector.parent,
                    )
                    parent.dispatchEvent(parentEvent)
                }
            }
        }

        addMouseListener {
            mouseExited {
                highlightPath = false
            }
            mouseClicked {
                if (highlightPath && it.clickCount == 2) {
                    showEditor()
                }
            }
        }

        attachPopupMenu {
            JPopupMenu().also { menu ->
                menu.add(
                    Action("Edit") { showEditor() },
                )
                menu.add(
                    Action("Delete") { deleteConnection() },
                )
            }
        }

        SwingUtilities.invokeLater {
            mouseObserver.start()
        }
    }

    private val hostNameChangeListener = ServiceModelChangeListener {
        val hostNameEnvVar = ConnectionDefinition.GATEWAY_NETWORK_X_HOST.name.replace("X", "$index")
        if (to.model.hostName != null && to.model.hostName != from.model.environment[hostNameEnvVar]) {
            from.model.environment[hostNameEnvVar] = to.model.hostName!!
            from.fireServiceModelChangedEvent()
        }
    }

    private val nodeDeleteListener = NodeDeleteListener {
        deleteConnection()
    }

    private fun finalizeConnection() {
        mouseObserver.stop()

        from.apply {
            model.environment.addOrRemove(
                createConnectionVariable(
                    ConnectionDefinition.GATEWAY_NETWORK_X_HOST,
                    index,
                    to.model.hostName,
                ),
            )

            addNodeDeleteListener {
                canvas.remove(this@GatewayNodeConnector)
            }

            fireServiceModelChangedEvent()
        }

        to.apply {
            addServiceModelChangeListener(hostNameChangeListener)
            addNodeDeleteListener(nodeDeleteListener)
        }
    }

    private fun deleteConnection() {
        val variablesToRemove = from.model.environment.keys.filter { key ->
            key.startsWith("GATEWAY_NETWORK_$index")
        }

        for (v in variablesToRemove) {
            from.model.environment.remove(v)
        }

        from.connections.remove(index)
        to.removeServiceModelChangeListener(hostNameChangeListener)

        canvas.remove(this)
        canvas.repaint()
    }

    private fun showEditor() = jFrame("Edit Connection", 485, 650) {
        contentPane = settingsPanel
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        if (g !is Graphics2D) return

        val b = boundsFromConnectionPoints
        if (b.width > 0 && b.height > 0) {
            bounds = b
        }

        drawLine(g)

        if (::to.isInitialized) {
            drawArrowIndicator(g)
        }
    }

    private fun drawLine(g: Graphics2D) {
        val lineThickness = if (highlightPath) 3F else 1F
        g.stroke = BasicStroke(lineThickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

        val fromPoint = SwingUtilities.convertPoint(from, from.midPoint(), this)
        val toPoint = if (connectionInProgress) {
            val p = mouseLocation.clone() as Point
            SwingUtilities.convertPointFromScreen(p, this)
            p
        } else {
            to.midPoint().convert(to, this)
        }

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

        val fromBounds = from.bounds.apply {
            width -= 1
            height -= 1
        }

        val allPoints = buildList {
            val curvePoints = connectionPath.getPathIterator(null, 0.01)
            while (!curvePoints.isDone) {
                val arr = DoubleArray(6)
                curvePoints.currentSegment(arr)
                add(Point(arr[0].toInt(), arr[1].toInt()))
                curvePoints.next()
            }
        }

        val exitPoint = allPoints.find {
            !from.bounds.contains(
                it.convert(this, getUnwrappedParent(this)),
            )
        } ?: return

        val canvasPoint = exitPoint.convert(this, getUnwrappedParent(this))

        val outcode = fromBounds.outcode(canvasPoint)
        val toIsAbove = toPoint.y < fromPoint.y
        val toIsLeft = toPoint.x < fromPoint.x

        var outerXOffset = 0
        var outerYOffset = 0

        var innerXOffset = 0
        var innerYOffset = 0

        when (outcode) {
            OUT_LEFT -> {
                outerXOffset = -curveWidth
                outerYOffset = if (toIsAbove) curveWidth else -curveWidth

                innerXOffset = curveWidth
                innerYOffset = if (toIsAbove) -curveWidth else curveWidth
            }

            OUT_TOP -> {
                outerXOffset = if (toIsLeft) curveWidth else -curveWidth
                outerYOffset = -curveWidth

                innerXOffset = if (toIsLeft) -curveWidth else curveWidth
                innerYOffset = curveWidth
            }

            OUT_RIGHT -> {
                outerXOffset = curveWidth
                outerYOffset = if (toIsAbove) curveWidth else -curveWidth

                innerXOffset = -curveWidth
                innerYOffset = if (toIsAbove) -curveWidth else curveWidth
            }

            OUT_BOTTOM -> {
                outerXOffset = if (toIsLeft) curveWidth else -curveWidth
                outerYOffset = curveWidth

                innerXOffset = if (toIsLeft) -curveWidth else curveWidth
                innerYOffset = -curveWidth
            }
        }
        g.draw(connectionPath)
        boundingPath.apply {
            reset()
            moveTo(fromPoint.x.toDouble() + outerXOffset, fromPoint.y.toDouble() + outerYOffset)
            curveTo(
                fromPoint.x.toDouble() + outerXOffset,
                toPoint.y.toDouble() + outerYOffset,
                fromPoint.x.toDouble() + outerXOffset,
                toPoint.y.toDouble() + outerYOffset,
                toPoint.x.toDouble() + outerXOffset,
                toPoint.y.toDouble() + outerYOffset,
            )
            lineTo(
                toPoint.x.toDouble() + innerXOffset,
                toPoint.y.toDouble() + innerYOffset,
            )
            curveTo(
                fromPoint.x.toDouble() + innerXOffset,
                toPoint.y.toDouble() + innerYOffset,
                fromPoint.x.toDouble() + innerXOffset,
                toPoint.y.toDouble() + innerYOffset,
                fromPoint.x.toDouble() + innerXOffset,
                fromPoint.y.toDouble() + innerYOffset,
            )
        }
    }

    private fun drawArrowIndicator(g: Graphics2D) {
        val allPoints = buildList {
            val curvePoints = connectionPath.getPathIterator(null, 0.01)
            while (!curvePoints.isDone) {
                val arr = DoubleArray(6)
                curvePoints.currentSegment(arr)
                add(Point(arr[0].toInt(), arr[1].toInt()))
                curvePoints.next()
            }
        }

        val arrowPoint = allPoints.findLast {
            !to.bounds.contains(
                it.convert(this, getUnwrappedParent(this)),
            )
        } ?: return

        val canvasPoint = arrowPoint.convert(this, getUnwrappedParent(this))
        val toBounds = to.bounds.apply {
            width -= 1
            height -= 1
        }

        val outcode = toBounds.outcode(canvasPoint)
        val toCoord = to.location.convert(getUnwrappedParent(to), this)

        val angle = when (outcode) {
            OUT_LEFT -> {
                arrowPoint.x = toCoord.x
                Math.PI / 2
            }

            OUT_TOP -> {
                arrowPoint.y = toCoord.y
                Math.PI
            }

            OUT_RIGHT -> {
                arrowPoint.x = toCoord.x + to.width
                Math.PI * 3 / 2
            }

            OUT_BOTTOM -> {
                arrowPoint.y = toCoord.y + to.height
                0.0
            }

            else -> {
                0.0
            }
        }

        val arrowHead = AffineTransform.getRotateInstance(
            angle,
            arrowPoint.x.toDouble(),
            arrowPoint.y.toDouble(),
        ).createTransformedShape(
            arrowHead(arrowPoint, 2),
        )

        g.fill(arrowHead)
    }

    private fun arrowHead(headPoint: Point, scale: Int = 1): Shape {
        val (headX, headY) = headPoint
        val xCoords = arrayOf(
            headX,
            headX - (3 * scale),
            headX,
            headX + (3 * scale),
            headX,
        ).toIntArray()

        val yCoords = arrayOf(
            headY,
            headY + (6 * scale),
            headY + (5 * scale),
            headY + (6 * scale),
            headY,
        ).toIntArray()

        return Polygon(xCoords, yCoords, 5)
    }

    companion object {
        fun JComponent.midPoint(): Point {
            return Point(width / 2, height / 2)
        }

        private fun boldJLabel(text: String? = null) = JLabel(text).apply {
            font = font.deriveFont(Font.BOLD)
        }
    }


    inner class ConnectionSettingsPanel : JPanel(MigLayout("fill, ins 4")) {
        private val arrowLabel = JLabel("→").apply {
            font = font.deriveFont(24F)
        }

        private val fromGatewayName = JLabel(from.model.containerName).apply {
            font = font.deriveFont(Font.BOLD, 14F)
        }
        private val fromGatewayHostname = JLabel().apply {
            text = buildString {
                tag("html") {
                    tag("b") {
                        append("Hostname: ")
                    }
                    append(from.model.hostName)
                }
            }
        }
        private val fromVersionLabel = JLabel().apply {
            text = buildString {
                tag("html") {
                    tag("b") {
                        append("Version: ")
                    }
                    append(from.model.version)
                }
            }
        }
        private val fromSection = JPanel(MigLayout("ins 4, fill")).apply {
            add(fromGatewayName, "span, alignx center")
            add(fromGatewayHostname, "span, alignx center")
            add(fromVersionLabel, "span, alignx center")
        }

        private val toGatewayName = JLabel(to.model.containerName).apply {
            font = font.deriveFont(Font.BOLD, 14F)
        }
        private val toGatewayHostname = JLabel("Hostname: ${to.model.hostName}").apply {
            text = buildString {
                tag("html") {
                    tag("b") {
                        append("Hostname: ")
                    }
                    append(to.model.hostName)
                }
            }
        }
        private val toVersionLabel = JLabel().apply {
            text = buildString {
                tag("html") {
                    tag("b") {
                        append("Version: ")
                    }
                    append(to.model.version)
                }
            }
        }
        private val toSection = JPanel(MigLayout("ins 4, fill")).apply {
            add(toGatewayName, "span, alignx center")
            add(toGatewayHostname, "span, alignx center")
            add(toVersionLabel, "span, alignx center")
        }

        val hostLabel = boldJLabel("Host")
        val hostField = JTextField(to.model.hostName).apply {
            isEditable = false
            isEnabled = false
        }

        val portLabel = boldJLabel("Port")
        val portField = JTextField(from.model.environment["GATEWAY_NETWORK_${index}_PORT"] ?: "8060")

        val pingRateLabel = boldJLabel("Ping Rate")
        val pingRateField = JSpinner(
            SpinnerNumberModel(
                from.model.environment["GATEWAY_NETWORK_${index}_PINGRATE"]?.toInt() ?: 1000,
                0,
                Integer.MAX_VALUE,
                1,
            ),
        )

        val pingMaxMissedLabel = boldJLabel("Ping Max Missed")
        val pingMaxMissedField = JSpinner(
            SpinnerNumberModel(
                from.model.environment["GATEWAY_NETWORK_${index}_PINGMAXMISSED"]?.toInt() ?: 30,
                -1,
                Integer.MAX_VALUE,
                1,
            ),
        )

        val enabledLabel = boldJLabel("Connection Enabled")
        val enabledCheckbox = JCheckBox().apply {
            isSelected = from.model.environment["GATEWAY_NETWORK_${index}_ENABLED"]?.toBoolean() ?: true
        }

        val enableSSLLabel = boldJLabel("Enable SSL")
        val enableSSLCheckbox = JCheckBox().apply {
            isSelected = from.model.environment["GATEWAY_NETWORK_${index}_ENABLESSL"]?.toBoolean() ?: true
        }

        val webSocketTimeoutLabel = boldJLabel("Websocket Timeout")
        val webSocketTimeoutField = JSpinner(
            SpinnerNumberModel(
                from.model.environment["GATEWAY_NETWORK_${index}_WEBSOCKETTIMEOUT"]?.toInt() ?: 10000,
                10,
                2_000_000_000,
                1000,
            ),
        )

        val descriptionLabel = boldJLabel("<html>Description<sup><i>8.1.26+</i></sup></html>")
        val descriptionEntry = JTextArea().apply {
            text = from.model.environment["GATEWAY_NETWORK_${index}_DESCRIPTION"] ?: ""
            isEnabled = IgnitionVersionComparator.compare(from.model.version, "8.1.26") >= 0
        }

        private val configSection =
            object : ConfigSection("Connection Configuration", "fill, ins 0, gap 10, ins 5 0 0 0") {
                init {
                    add(hostLabel, "sg")
                    add(hostField, "pushx, growx, wrap")
                    add(portLabel, "sg")
                    add(portField, "pushx, growx, wrap")
                    add(pingRateLabel, "sg")
                    add(pingRateField, "pushx, growx, wrap")
                    add(pingMaxMissedLabel, "sg")
                    add(pingMaxMissedField, "pushx, growx, wrap")
                    add(enabledLabel, "sg")
                    add(enabledCheckbox, "pushx, growx, wrap")
                    add(enableSSLLabel, "sg")
                    add(enableSSLCheckbox, "pushx, growx, wrap")
                    add(webSocketTimeoutLabel, "sg")
                    add(webSocketTimeoutField, "pushx, growx, wrap")
                    add(descriptionLabel, "sg, wrap")
                    add(descriptionEntry, "push, grow, span")
                }
            }

        private val closeButton = JButton("Cancel").apply {
            addActionListener {
                SwingUtilities.getWindowAncestor(this@ConnectionSettingsPanel).dispose()
            }
        }

        private val applyButton = JButton("Apply").apply {
            addActionListener {
                applySettings()
            }
        }

        private val okButton = JButton("OK").apply {
            addActionListener {
                applySettings()
                SwingUtilities.getWindowAncestor(this@ConnectionSettingsPanel).dispose()
            }
        }

        private val footer = JPanel(MigLayout("fill, ins 0")).apply {
            add(closeButton, "west")
            add(okButton, "east")
            add(applyButton, "east")
        }

        init {
            add(fromSection, "pushx, growx, sg")
            add(fromSection, "pushx, growx, sg")
            add(arrowLabel, "growx")
            add(toSection, "pushx, growx, sg, wrap")
            add(configSection, "push, grow, span, gaptop 10")
            add(footer, "growx, spanx")
        }

        private fun applySettings() {
            val variables = listOf(
                createConnectionVariable(
                    ConnectionDefinition.GATEWAY_NETWORK_X_PORT,
                    index,
                    portField.text,
                ),
                createConnectionVariable(
                    ConnectionDefinition.GATEWAY_NETWORK_X_PINGRATE,
                    index,
                    pingRateField.value.toString(),
                ),
                createConnectionVariable(
                    ConnectionDefinition.GATEWAY_NETWORK_X_PINGMAXMISSED,
                    index,
                    pingMaxMissedField.value.toString(),
                ),
                createConnectionVariable(
                    ConnectionDefinition.GATEWAY_NETWORK_X_ENABLED,
                    index,
                    enabledCheckbox.isSelected.toString(),
                ),
                createConnectionVariable(
                    ConnectionDefinition.GATEWAY_NETWORK_X_ENABLESSL,
                    index,
                    enableSSLCheckbox.isSelected.toString(),
                ),
                createConnectionVariable(
                    ConnectionDefinition.GATEWAY_NETWORK_X_WEBSOCKETTIMEOUT,
                    index,
                    webSocketTimeoutField.value.toString(),
                ),
                createConnectionVariable(
                    ConnectionDefinition.GATEWAY_NETWORK_X_DESCRIPTION,
                    index,
                    descriptionEntry.text,
                ),
            )

            for (v in variables) {
                from.model.environment.addOrRemove(v)
            }

            from.fireServiceModelChangedEvent()
        }
    }
}
