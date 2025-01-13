package io.github.inductiveautomation.kindling.utils

import com.formdev.flatlaf.extras.FlatSVGIcon
import com.github.weisj.jsvg.SVGDocument
import com.github.weisj.jsvg.attributes.ViewBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.jdesktop.swingx.decorator.ColorHighlighter
import org.jdesktop.swingx.decorator.ComponentAdapter
import org.jdesktop.swingx.decorator.HighlightPredicate
import org.jdesktop.swingx.prompt.BuddySupport
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.image.BufferedImage
import java.io.File
import java.util.EventListener
import javax.swing.InputVerifier
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JPopupMenu
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.EventListenerList
import javax.swing.text.Document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.jdesktop.swingx.prompt.BuddySupport

/**
 * A common CoroutineScope bound to the event dispatch thread (see [Dispatchers.Swing]).
 */
val EDT_SCOPE by lazy { CoroutineScope(Dispatchers.Swing) }

val menuShortcutKeyMaskEx = Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx

val Document.text: String
    get() = getText(0, length)

inline fun <T : Component> T.attachPopupMenu(
    crossinline menuFn: T.(event: MouseEvent) -> JPopupMenu?,
) {
    addMouseListener(
        object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                maybeShowPopup(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                maybeShowPopup(e)
            }

            private fun maybeShowPopup(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    e.consume()
                    menuFn.invoke(this@attachPopupMenu, e)?.show(e.component, e.x, e.y)
                }
            }
        },
    )
}

fun FlatSVGIcon.asActionIcon(selected: Boolean = false): FlatSVGIcon {
    return FlatSVGIcon(name, 0.75F).apply {
        if (selected) {
            colorFilter = FlatSVGIcon.ColorFilter { UIManager.getColor("Tree.selectionForeground") }
        }
    }
}

fun JFileChooser.chooseFiles(parent: JComponent?): List<File>? {
    return if (showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
        selectedFiles.toList()
    } else {
        null
    }
}

inline fun <reified T : EventListener> EventListenerList.add(listener: T) {
    add(T::class.java, listener)
}

inline fun <reified T : EventListener> EventListenerList.remove(listener: T) {
    remove(T::class.java, listener)
}

inline fun <reified T : EventListener> EventListenerList.getAll(): Array<T> {
    return getListeners(T::class.java)
}

fun Component.traverseChildren(recursive: Boolean = true): Sequence<Component> = sequence {
    if (this@traverseChildren is Container) {
        val childComponents = synchronized(treeLock) { components.copyOf() }
        for (component in childComponents) {
            yield(component)
            if (recursive) yieldAll(component.traverseChildren())
        }
    }
}

fun SVGDocument.render(width: Int, height: Int, x: Int = 0, y: Int = 0): BufferedImage {
    return BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).apply {
        val g = createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        render(null as Component?, g, ViewBox(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()))
        g.dispose()
    }
}

inline fun <reified C> Component.getAncestorOfClass(): C? {
    return SwingUtilities.getAncestorOfClass(C::class.java, this) as? C
}

var JTextField.leftBuddy: JComponent?
    get() {
        return BuddySupport.getLeft(this)?.firstOrNull() as? JComponent
    }
    set(buddy) {
        BuddySupport.addLeft(buddy, this)
    }

var JTextField.rightBuddy: JComponent?
    get() {
        return BuddySupport.getRight(this)?.firstOrNull() as? JComponent
    }
    set(buddy) {
        BuddySupport.addRight(buddy, this)
    }

@Suppress("FunctionName")
fun DocumentAdapter(block: (e: DocumentEvent) -> Unit): DocumentListener = object : DocumentListener {
    override fun changedUpdate(e: DocumentEvent) = block(e)
    override fun insertUpdate(e: DocumentEvent) = block(e)
    override fun removeUpdate(e: DocumentEvent) = block(e)
}

data class ColorPalette(
    val background: Color?,
    val foreground: Color?,
) {
    fun toHighLighter(
        predicate: (component: Component, componentAdapter: ComponentAdapter) -> Boolean = { _, _ -> true },
    ): ColorHighlighter {
        return ColorHighlighter(background, foreground, predicate)
    }
}

fun ColorHighlighter(
    background: Color?,
    foreground: Color?,
    predicate: (component: Component, componentAdapter: ComponentAdapter) -> Boolean = { _, _ -> true },
) = ColorHighlighter(HighlightPredicate(predicate), background, foreground)

@OptIn(ExperimentalStdlibApi::class)
fun Color.toHexString(alpha: Boolean = false): String {
    val hexString = rgb.toHexString()
    return "#${
        if (alpha) {
            hexString
        } else {
            hexString.substring(2)
        }
    }"
}

inline fun <reified T : JComponent> InputVerifier(
    crossinline verify: (T) -> Boolean
): InputVerifier {
    return object : InputVerifier() {
        override fun verify(input: JComponent?): Boolean {
            return input is T && verify(input)
        }
    }
}

@Suppress("unused")
class MouseListenerBuilder : MouseListener {
    fun mouseClicked(block: (e: MouseEvent) -> Unit) { this.mouseClicked = block }
    fun mousePressed(block: (e: MouseEvent) -> Unit) { this.mousePressed = block }
    fun mouseReleased(block: (e: MouseEvent) -> Unit) { this.mouseReleased = block }
    fun mouseEntered(block: (e: MouseEvent) -> Unit) { this.mouseEntered = block }
    fun mouseExited(block: (e: MouseEvent) -> Unit) { this.mouseExited = block }

    private var mouseClicked: (e: MouseEvent) -> Unit = {}
    private var mousePressed: (e: MouseEvent) -> Unit = {}
    private var mouseReleased: (e: MouseEvent) -> Unit = {}
    private var mouseEntered: (e: MouseEvent) -> Unit = {}
    private var mouseExited: (e: MouseEvent) -> Unit = {}

    override fun mouseClicked(e: MouseEvent?) = e?.let(mouseClicked::invoke) ?: Unit
    override fun mousePressed(e: MouseEvent?) = e?.let(mousePressed::invoke) ?: Unit
    override fun mouseReleased(e: MouseEvent?) = e?.let(mouseReleased::invoke) ?: Unit
    override fun mouseEntered(e: MouseEvent?) = e?.let(mouseEntered::invoke) ?: Unit
    override fun mouseExited(e: MouseEvent?) = e?.let(mouseExited::invoke) ?: Unit

    companion object {
        fun Component.addMouseListener(block: MouseListenerBuilder.() -> Unit) {
            addMouseListener(MouseListenerBuilder().apply(block))
        }
    }
}

@Suppress("unused")
class MouseMotionListenerBuilder : MouseMotionListener {
    fun mouseDragged(block: (e: MouseEvent) -> Unit) { this.mouseDragged = block }
    fun mouseMoved(block: (e: MouseEvent) -> Unit) { this.mouseMoved = block }

    private var mouseDragged: (e: MouseEvent) -> Unit = {}
    private var mouseMoved: (e: MouseEvent) -> Unit = {}

    override fun mouseDragged(e: MouseEvent?) = e?.let(mouseDragged::invoke) ?: Unit
    override fun mouseMoved(e: MouseEvent?) = e?.let(mouseMoved::invoke) ?: Unit

    companion object {
        fun JComponent.addMouseMotionListener(block: MouseMotionListenerBuilder.() -> Unit) {
            addMouseMotionListener(MouseMotionListenerBuilder().apply(block))
        }
    }
}