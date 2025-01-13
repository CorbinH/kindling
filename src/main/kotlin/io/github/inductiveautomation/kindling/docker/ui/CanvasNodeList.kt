package io.github.inductiveautomation.kindling.docker.ui

import io.github.inductiveautomation.kindling.docker.DockerTool
import io.github.inductiveautomation.kindling.utils.listCellRenderer
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import javax.swing.AbstractListModel
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.TransferHandler

class CanvasNodeList : JList<NodeInitializer>(CanvasNodeListModel()) {
    override fun getModel(): CanvasNodeListModel = super.getModel() as CanvasNodeListModel

    init {
        dragEnabled = true
        cellRenderer = listCellRenderer<NodeInitializer> { _, value, _, _, _ ->
            text = model[value]
            icon = DockerTool.icon
        }

        transferHandler = NodeInitializerTransferHandler()
    }

    class CanvasNodeListModel : AbstractListModel<NodeInitializer>() {

        private val data: List<NodeInitializer> = listOf(
            NodeInitializer { GenericDockerServiceNode() },
            NodeInitializer { GatewayServiceNode() },
        )

        operator fun get(i: NodeInitializer): String {
            return when (data.indexOf(i)) {
                0 -> "Generic Service Node"
                1 -> "Ignition Gateway Node"
                else -> error("Invalid list argument.")
            }
        }

        override fun getSize(): Int  = data.size

        override fun getElementAt(index: Int): NodeInitializer = data[index]
    }
}

fun interface NodeInitializer {
    fun initialize(): AbstractDockerServiceNode<*>
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

    override fun exportDone(source: JComponent?, data: Transferable?, action: Int) {
        super.exportDone(source, data, action)
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
