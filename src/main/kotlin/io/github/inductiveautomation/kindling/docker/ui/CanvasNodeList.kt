package io.github.inductiveautomation.kindling.docker.ui

import io.github.inductiveautomation.kindling.utils.listCellRenderer
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import javax.swing.AbstractListModel
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.TransferHandler

class CanvasNodeList(
    initializers: List<NodeInitializer>,
) : JList<NodeInitializer>(CanvasNodeListModel(initializers)) {
    override fun getModel(): CanvasNodeListModel = super.getModel() as CanvasNodeListModel

    init {
        dragEnabled = true
        cellRenderer = listCellRenderer<NodeInitializer> { _, value, _, _, _ ->
            text = value.description
            icon = value.icon
        }

        transferHandler = NodeInitializerTransferHandler()
    }

    class CanvasNodeListModel(private val data: List<NodeInitializer>) : AbstractListModel<NodeInitializer>() {
        override fun getSize(): Int = data.size

        override fun getElementAt(index: Int): NodeInitializer = data[index]
    }
}

interface NodeInitializer {
    val icon: Icon
    val description: String
    fun initialize(): AbstractDockerServiceNode<*>

    companion object {
        operator fun invoke(icon: Icon, description: String, init: () -> AbstractDockerServiceNode<*>): NodeInitializer {
            return object : NodeInitializer {
                override val icon = icon
                override val description = description
                override fun initialize(): AbstractDockerServiceNode<*> = init()
            }
        }
    }
}

class NodeInitializerTransferHandler : TransferHandler() {
    // Export
    override fun getSourceActions(c: JComponent?): Int = COPY

    override fun createTransferable(c: JComponent?): Transferable? {
        if (c is JList<*>) {
            val selectedInitializer = c.selectedValue as? NodeInitializer

            if (selectedInitializer != null) {
                return NodeInitializerTransferable(selectedInitializer)
            }
        }

        return null
    }

    class NodeInitializerTransferable(
        private val initializer: NodeInitializer,
    ) : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> {
            return arrayOf(NODE_INITIALIZER_DATA_FLAVOR)
        }

        override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
            return flavor == NODE_INITIALIZER_DATA_FLAVOR
        }

        override fun getTransferData(flavor: DataFlavor?): Any {
            return initializer
        }
    }

    companion object {
        val NODE_INITIALIZER_DATA_FLAVOR = DataFlavor(
            NodeInitializer::class.java,
            "node initializer",
        )
    }
}
