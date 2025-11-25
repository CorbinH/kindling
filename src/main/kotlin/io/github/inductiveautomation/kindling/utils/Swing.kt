package io.github.inductiveautomation.kindling.utils

import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.extras.components.FlatTextField
import com.github.weisj.jsvg.SVGDocument
import com.github.weisj.jsvg.view.ViewBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.swing.Swing
import org.jdesktop.swingx.decorator.AbstractHighlighter
import org.jdesktop.swingx.decorator.ColorHighlighter
import org.jdesktop.swingx.decorator.ComponentAdapter
import org.jdesktop.swingx.decorator.Highlighter
import org.jdesktop.swingx.prompt.BuddySupport
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Point
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.image.BufferedImage
import java.io.File
import java.util.EventListener
import javax.swing.InputVerifier
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.EventListenerList
import javax.swing.text.Document
import javax.swing.text.JTextComponent

/**
 * A common CoroutineScope bound to the event dispatch thread (see [Dispatchers.Swing]).
 */
val EDT_SCOPE: CoroutineScope = CoroutineScope(Dispatchers.Swing) + SupervisorJob()

val menuShortcutKeyMaskEx = Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx

/**
 * A convenience property to get or set the contents of the system clipboard as a string.
 */
var Toolkit.clipboardString: String?
    get() {
        return runCatching {
            systemClipboard.getData(DataFlavor.stringFlavor) as? String
        }.getOrNull()
    }
    set(value) {
        systemClipboard.setContents(StringSelection(value), null)
    }

val Document.text: String
    get() = getText(0, length)

fun JFrame.dismissOnEscape() {
    rootPane.actionMap.put(
        "dismiss",
        Action {
            dispose()
        },
    )
    rootPane.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "dismiss")
}

inline fun <T : Component> T.attachPopupMenu(crossinline menuFn: T.(event: MouseEvent) -> JPopupMenu?) {
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

const val ACTION_ICON_SCALE_FACTOR = 0.75F

@Suppress("FunctionName")
fun FlatActionIcon(path: String): FlatSVGIcon = FlatSVGIcon(path, ACTION_ICON_SCALE_FACTOR)

fun JFileChooser.chooseFiles(parent: JComponent?): List<File>? {
    return if (showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
        if (isMultiSelectionEnabled) selectedFiles.toList() else listOf(selectedFile)
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

inline fun <reified T : EventListener> EventListenerList.getAll(): Array<T> = getListeners(T::class.java)

fun Component.traverseChildren(recursive: Boolean = true): Sequence<Component> = sequence {
    if (this@traverseChildren is Container) {
        val childComponents = synchronized(treeLock) { components.copyOf() }
        for (component in childComponents) {
            yield(component)
            if (recursive) yieldAll(component.traverseChildren())
        }
    }
}

fun SVGDocument.render(width: Int, height: Int, x: Int = 0, y: Int = 0): BufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).apply {
    val g = createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    render(null as Component?, g, ViewBox(x.toFloat(), y.toFloat(), width.toFloat(), height.toFloat()))
    g.dispose()
}

inline fun <reified C> Component.getAncestorOfClass(): C? = SwingUtilities.getAncestorOfClass(C::class.java, this) as? C

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

fun JScrollPane.scrollToTop() = EDT_SCOPE.launch {
    viewport.viewPosition = Point(0, 0)
}

@Suppress("FunctionName")
fun DocumentAdapter(block: (e: DocumentEvent) -> Unit): DocumentListener = object : DocumentListener {
    override fun changedUpdate(e: DocumentEvent) = block(e)
    override fun insertUpdate(e: DocumentEvent) = block(e)
    override fun removeUpdate(e: DocumentEvent) = block(e)
}

fun Document.onChange(block: (String) -> Unit) {
    addDocumentListener(
        DocumentAdapter {
            block(text)
        }
    )
}

typealias HighlightPredicateKt = (component: Component, adapter: ComponentAdapter) -> Boolean

data class ColorPalette(
    val background: Color?,
    val foreground: Color?,
) {
    fun toHighLighter(
        predicate: HighlightPredicateKt = { _, _ -> true },
    ): ColorHighlighter = ColorHighlighter(predicate, background, foreground)
}

fun ColorHighlighter(
    background: Color?,
    foreground: Color?,
    predicate: HighlightPredicateKt = { _, _ -> true },
) = ColorHighlighter(predicate, background, foreground)

@Suppress("FunctionName")
fun ColorHighlighter(
    fgSupplier: (() -> Color)?,
    bgSupplier: (() -> Color)?,
    predicate: HighlightPredicateKt = { _, _ -> true },
): Highlighter = object : AbstractHighlighter(predicate) {
    override fun doHighlight(
        target: Component,
        adapter: ComponentAdapter,
    ): Component = target.apply {
        fgSupplier?.invoke()?.let { foreground = it }
        bgSupplier?.invoke()?.let { background = it }
    }
}

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
    crossinline verify: (T) -> Boolean,
): InputVerifier {
    return object : InputVerifier() {
        override fun verify(input: JComponent?): Boolean {
            return input is T && verify(input)
        }
    }
}

class RegexInputVerifier(
    private val regex: Regex,
    private val allowPartialMatch: Boolean = false,
) : InputVerifier() {
    override fun verify(input: JComponent?): Boolean {
        if (input is JTextComponent) {
            return if (allowPartialMatch) {
                regex.containsMatchIn(input.text)
            } else {
                regex.matches(input.text)
            }
        } else if (input is JComboBox<*>) {
            val strInput = input.selectedItem as? String ?: return false
            return if (allowPartialMatch) {
                regex.containsMatchIn(strInput)
            } else {
                regex.matches(strInput)
            }
        } else {
            return false
        }
    }
}

@Suppress("unused")
class MouseListenerBuilder : MouseListener {
    fun mouseClicked(block: (e: MouseEvent) -> Unit) {
        this.mouseClicked = block
    }
    fun mousePressed(block: (e: MouseEvent) -> Unit) {
        this.mousePressed = block
    }
    fun mouseReleased(block: (e: MouseEvent) -> Unit) {
        this.mouseReleased = block
    }
    fun mouseEntered(block: (e: MouseEvent) -> Unit) {
        this.mouseEntered = block
    }
    fun mouseExited(block: (e: MouseEvent) -> Unit) {
        this.mouseExited = block
    }

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

class MouseMotionListenerBuilder : MouseMotionListener {
    fun mouseDragged(block: (e: MouseEvent) -> Unit) {
        this.mouseDragged = block
    }
    fun mouseMoved(block: (e: MouseEvent) -> Unit) {
        this.mouseMoved = block
    }

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

object PointHelpers {
    operator fun Point.component1() = x
    operator fun Point.component2() = y

    fun Point.convert(from: Component?, to: Component?): Point {
        return SwingUtilities.convertPoint(from, this, to)
    }
}

val ListSelectionModel.minSelectedIndex: Int?
    get() = minSelectionIndex.takeIf { it != -1 }

val ListSelectionModel.maxSelectedIndex: Int?
    get() = maxSelectionIndex.takeIf { it != -1 }

fun FlatTextField.attachValidator(validator: (s: String?) -> Boolean) {
    inputVerifier = object : InputVerifier() {
        override fun shouldYieldFocus(source: JComponent?, target: JComponent?) = true
        override fun verify(input: JComponent?): Boolean {
            return validator((input as? JTextField)?.text)
        }
    }
    document.addDocumentListener(object : DocumentListener {
        private fun validate() {
            outline = if (inputVerifier.verify(this@attachValidator)) null else "error"
        }

        override fun insertUpdate(e: DocumentEvent?) {
            validate()
        }

        override fun removeUpdate(e: DocumentEvent?) {
            validate()
        }

        override fun changedUpdate(e: DocumentEvent?) {
            validate()
        }

        init {
            validate()
        }
    })
}
