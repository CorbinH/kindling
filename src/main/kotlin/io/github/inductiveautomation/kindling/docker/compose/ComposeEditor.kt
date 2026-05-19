package io.github.inductiveautomation.kindling.docker.compose

import com.formdev.flatlaf.extras.FlatSVGIcon
import io.github.inductiveautomation.kindling.utils.AbstractTreeNode
import io.github.inductiveautomation.kindling.utils.Action
import io.github.inductiveautomation.kindling.utils.FlatScrollPane
import io.github.inductiveautomation.kindling.utils.KMutableListModel
import io.github.inductiveautomation.kindling.utils.NumericEntryField
import io.github.inductiveautomation.kindling.utils.ReifiedJXTable
import io.github.inductiveautomation.kindling.utils.ReifiedMapTableModel
import io.github.inductiveautomation.kindling.utils.StringPairColumns
import io.github.inductiveautomation.kindling.utils.TrivialListDataListener
import io.github.inductiveautomation.kindling.utils.minSelectedIndex
import io.github.inductiveautomation.kindling.utils.text
import io.github.inductiveautomation.kindling.utils.traverseChildren
import net.miginfocom.swing.MigLayout
import org.jdesktop.swingx.JXTextArea
import java.awt.Font
import java.awt.event.ItemEvent
import java.text.NumberFormat
import java.util.EventListener
import javax.swing.BorderFactory
import javax.swing.InputVerifier
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JFormattedTextField
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.border.MatteBorder
import javax.swing.border.TitledBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.ListDataEvent
import javax.swing.event.TableModelEvent
import kotlin.enums.EnumEntries
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

sealed class ComposeEditor : AbstractTreeNode() {
    abstract val name: String
    abstract val component: JComponent
    val root: RootEditor?
        get() {
            var p: AbstractTreeNode? = this
            while (p != null && p !is RootEditor) {
                p = p.parent
            }
            return p
        }
}

/**
 * Interface reserved for compose objects which are at the root of a hierarchy.
 */
interface RootEditor {
    fun interface ValueChangeListener : EventListener {
        fun valueChange()
    }

    fun addValueChangeListener(l: ValueChangeListener)

    fun fireValueChanged()
}

sealed class ComposeObjectEditor<T>(
    override val name: String,
    val data: T,
) : ComposeEditor() {
    private val _component by lazy {
        val container = JPanel(MigLayout("fillx, ins 10, flowy, aligny top, gapy 20")).apply {
            border = MatteBorder(0, 2, 0, 0, UIManager.getColor("Button.background"))
        }
        val headerButton = JButton(name).apply {
            horizontalAlignment = SwingConstants.LEFT
            addActionListener {
                container.isVisible = !container.isVisible
            }
        }

        JPanel(MigLayout("fill, ins 0, hidemode 3")).apply {
            add(headerButton, "growx, aligny top")
            add(container, "newline, push, grow")
        }
    }

    final override val component: JComponent
        get() = _component.apply {
            val container = getComponent(1) as JPanel
            for (editor in children) {
                editor as ComposeEditor
                container.add(editor.component, "grow")
            }
        }

    protected fun <T> composeObject(
        obj: ComposeObjectEditor<T>,
    ) = PropertyDelegateProvider { thisRef: ComposeObjectEditor<*>, _ ->
        thisRef.children.add(obj)
        ReadOnlyProperty<ComposeObjectEditor<*>, _> { _, _ -> obj }
    }

    protected fun composeValue(
        name: String? = null,
        tip: String? = null,
        newline: Boolean = true,
        component: ComposeValueEditor.() -> JComponent,
    ) = PropertyDelegateProvider { thisRef: ComposeObjectEditor<*>, property ->
        val upper = name ?: property.name.replaceFirstChar { it.uppercase() }
        val editor = ComposeValueEditor(upper, tip, newline, getComponent = component)
        thisRef.children.add(editor)
        ReadOnlyProperty<ComposeObjectEditor<*>, _> { _, _ -> editor }
    }

    /* Specific value functions to reduce duplicate code */
    protected inline fun <reified E : Enum<E>> combo(
        name: String? = null,
        options: EnumEntries<E>,
        initialValue: E,
        noinline onSelect: (E) -> Unit,
    ) = composeValue(name) {
        JComboBox(options.toTypedArray()).apply {
            selectedItem = initialValue
            addItemListener {
                if (it.stateChange == ItemEvent.SELECTED) {
                    onSelect(it.item as E)
                    root?.fireValueChanged()
                }
            }
        }
    }

    fun numeric(
        name: String? = null,
        initialValue: Int?,
        onChange: (Int?) -> Unit,
    ) = composeValue(name) {
        NumericEntryField(initialValue?.toLong()).apply {
            text = initialValue?.toString()
            addValueChangeListener {
                onChange(it?.toInt())
                root?.fireValueChanged()
            }
            horizontalAlignment = SwingConstants.RIGHT
        }
    }

    fun numeric(
        name: String? = null,
        initialValue: Float?,
        onChange: (Float?) -> Unit,
    ) = composeValue(name) {
        JFormattedTextField(NumberFormat.getNumberInstance()).apply {
            value = initialValue
            inputVerifier = object : InputVerifier() {
                override fun verify(input: JComponent?): Boolean {
                    if (input is JFormattedTextField) {
                        if (input.text.isBlank()) {
                            return true
                        }

                        val formatter = input.formatter
                        val formatValue = runCatching { formatter.stringToValue(input.text) }.getOrNull()

                        return formatValue == null
                    }
                    return true
                }

                override fun shouldYieldFocus(source: JComponent?, target: JComponent?): Boolean = verify(source)
            }

            addPropertyChangeListener("value") {
                onChange((it.newValue as Number).toFloat())
                root?.fireValueChanged()
            }
        }
    }

    fun text(
        name: String? = null,
        value: String?,
        onChange: (String?) -> Unit,
    ) = composeValue(name) {
        JTextField(value).apply {
            document.addDocumentListener(
                object : DocumentListener {
                    fun anyUpdate(e: DocumentEvent?) {
                        onChange(e?.document?.text?.ifBlank { null })
                        if (root == null) {
                            println("Not firing value change because root is null.")
                        }
                        root?.fireValueChanged()
                    }
                    override fun insertUpdate(e: DocumentEvent?) = anyUpdate(e)
                    override fun removeUpdate(e: DocumentEvent?) = anyUpdate(e)
                    override fun changedUpdate(e: DocumentEvent?) = anyUpdate(e)
                },
            )
        }
    }

    fun <T : Any> list(
        name: String? = null,
        value: MutableList<T>,
        createDefault: () -> T,
        configure: ListElementEditor<T>.() -> Unit,
    ) = composeValue(name) {
        val panel = JPanel(MigLayout("fillx, flowy, ins 0, gap 10")).apply {
            value.forEachIndexed { i, item ->
                add(
                    ListElementEditor(i, item) {
                        value.remove(it)
                        this@apply.remove(this)
                        root?.fireValueChanged()
                        this@apply.revalidate()
                        this@apply.repaint()

                        this@apply.traverseChildren(false).forEachIndexed { index, element ->
                            if (element is ListElementEditor<*>) {
                                element.index = index + 1
                            }
                        }
                    }.apply { configure() },
                    "growx",
                )
            }
        }

        val scrollPane = FlatScrollPane(panel)

        val addButton = JButton(
            Action("+", "Add new item") {
                val data = createDefault()
                val componentCount = synchronized(panel.treeLock) {
                    panel.componentCount
                }

                value.add(data)
                panel.add(
                    ListElementEditor(componentCount, data) {
                        value.remove(it)
                        panel.remove(this)
                        root?.fireValueChanged()
                        panel.revalidate()
                        panel.repaint()

                        panel.traverseChildren(false).forEachIndexed { index, element ->
                            if (element is ListElementEditor<*>) {
                                element.index = index + 1
                            }
                        }
                    }.apply { configure() },
                    "growx",
                    componentCount - 1,
                )

                root?.fireValueChanged()

                SwingUtilities.invokeLater {
                    /* This is hacky but it works... We need the scroll pane to grow when more elements have been added. */
                    val container = SwingUtilities.getUnwrappedParent(scrollPane)
                    container.revalidate()
                    container.repaint()
                }
            },
        )

        panel.add(addButton, "alignx center")

        scrollPane
    }

    fun list(
        name: String? = null,
        value: MutableList<String>,
        onChange: (event: ListDataEvent) -> Unit = {},
    ) = composeValue(name) {
        val textField = JTextField()
        val list = object : JList<String>(KMutableListModel(value)) {
            override fun getModel() = super.model as KMutableListModel<String>
            init {
                selectionMode = ListSelectionModel.SINGLE_SELECTION
            }
        }

        val addButton = JButton("+").apply {
            addActionListener {
                if (textField.text.isNotBlank()) {
                    list.model.add(textField.text)
                }
            }
        }
        val removeButton = JButton("-").apply {
            isEnabled = false
            addActionListener {
                val selection = list.selectionModel.minSelectedIndex
                if (selection != null) {
                    list.model.removeAt(selection)
                }
            }
        }

        list.selectionModel.addListSelectionListener {
            removeButton.isEnabled = !(it.source as ListSelectionModel).isSelectionEmpty
        }

        list.model.addListDataListener(
            TrivialListDataListener { event ->
                if (event != null) {
                    onChange(event)
                    root?.fireValueChanged()
                }
            },
        )

        JPanel(MigLayout("fill, ins 0")).apply {
            add(textField, "pushx, growx")
            add(removeButton)
            add(addButton, "wrap")
            add(list, "push, grow, span")
        }
    }

    fun checkbox(
        name: String? = null,
        value: Boolean,
        onChange: (Boolean) -> Unit,
    ) = composeValue(name, newline = false) {
        JCheckBox().apply {
            isSelected = value
            addActionListener {
                onChange(isSelected)
                root?.fireValueChanged()
            }
        }
    }

    fun map(
        name: String? = null,
        value: MutableMap<String, String>,
        onChange: (TableModelEvent) -> Unit = {},
    ) = composeValue(name) {
        val table = ReifiedJXTable(ReifiedMapTableModel(value, StringPairColumns)).apply {
            isColumnControlVisible = false
            isSortable = false

            model.addTableModelListener {
                onChange(it)
                root?.fireValueChanged()
            }
        }

        val header = JPanel(MigLayout("fill")).apply {
            val addRemove = JLabel("Add/Remove")
            val keyEntry = JXTextArea("Key")
            val valueEntry = JXTextArea("Value")

            val addButton = JButton("+").apply {
                addActionListener {
                    if (!keyEntry.text.isNullOrBlank() && !valueEntry.text.isNullOrBlank()) {
                        value[keyEntry.text] = valueEntry.text
                        table.model.fireTableDataChanged()
                    }
                }
            }

            val removeButton = JButton("-").apply {
                isEnabled = !table.selectionModel.isSelectionEmpty

                table.selectionModel.addListSelectionListener {
                    isEnabled = !(it.source as ListSelectionModel).isSelectionEmpty
                }

                addActionListener {
                    val entries = value.keys.toList()
                    val toRemove = table.selectionModel.selectedIndices.map {
                        entries[it]
                    }
                    toRemove.forEach { value.remove(it) }
                    table.model.fireTableDataChanged()
                }
            }

            add(addRemove, "west")
            add(keyEntry, "grow, sg")
            add(valueEntry, "grow, sg")
            add(addButton, "east")
            add(removeButton, "east")
        }

        JPanel(MigLayout("fill, ins 0")).apply {
            add(header, "growx, spanx")
            add(table, "push, grow")
        }
    }
}

class ComposeValueEditor(
    override val name: String,
    tip: String? = null,
    newline: Boolean = true,
    getComponent: ComposeValueEditor.() -> JComponent,
) : ComposeEditor() {
    override val component by lazy {
        JPanel(MigLayout("fillx, ins 0")).apply {
            add(JLabel(this@ComposeValueEditor.name), "gapleft 5, gapbottom 5").apply {
                font = font.deriveFont(Font.BOLD, 14f)
            }
            add(getComponent(), if (newline) "growx, newline" else "growx")

            toolTipText = tip
        }
    }
}

class ListElementEditor<T : Any>(
    initialIndex: Int,
    val element: T,
    onDelete: ListElementEditor<T>.(T) -> Unit,
) : JPanel(MigLayout("fill")) {
    init {
        border = BorderFactory.createTitledBorder("Element $initialIndex")
    }

    var index: Int = initialIndex
        set(value) {
            field = value
            (border as TitledBorder).title = "Element $value"
        }

    init {
        val removeButton = JButton(
            Action(description = "Delete item", icon = FlatSVGIcon("icons/bx-x.svg")) {
                onDelete(element)
            },
        )

        add(removeButton, "alignx right, span")

        removeButton.addActionListener {
            onDelete(element)
        }
    }
}
