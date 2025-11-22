package io.github.inductiveautomation.kindling.docker

import io.github.inductiveautomation.kindling.docker.services.DockerServiceTool
import io.github.inductiveautomation.kindling.utils.listCellRenderer
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import javax.swing.AbstractListModel
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.TransferHandler
import kotlin.jvm.java

class CanvasNodeList(
    initializers: List<DockerServiceTool>,
) : JList<DockerServiceTool>(CanvasNodeListModel(initializers)) {
    override fun getModel(): CanvasNodeListModel = super.getModel() as CanvasNodeListModel

    init {
        dragEnabled = true
        cellRenderer = listCellRenderer<DockerServiceTool> { _, value, _, _, _ ->
            text = value.name
            icon = value.icon
        }

        transferHandler = DockerServiceToolTransferHandler()
    }

    class CanvasNodeListModel(private val data: List<DockerServiceTool>) : AbstractListModel<DockerServiceTool>() {
        override fun getSize(): Int = data.size

        override fun getElementAt(index: Int): DockerServiceTool = data[index]
    }
}

class DockerServiceToolTransferHandler : TransferHandler() {
    // Export
    override fun getSourceActions(c: JComponent?): Int = COPY

    override fun createTransferable(c: JComponent?): Transferable? {
        if (c is JList<*>) {
            val selectedInitializer = c.selectedValue as? DockerServiceTool

            if (selectedInitializer != null) {
                return DockerServiceToolTransferable(selectedInitializer)
            }
        }

        return null
    }

    class DockerServiceToolTransferable(
        private val initializer: DockerServiceTool,
    ) : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> {
            return arrayOf(DOCKER_SERVICE_DATA_FLAVOR)
        }

        override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean {
            return flavor == DOCKER_SERVICE_DATA_FLAVOR
        }

        override fun getTransferData(flavor: DataFlavor?): Any {
            return initializer
        }
    }

    companion object {
        val DOCKER_SERVICE_DATA_FLAVOR = DataFlavor(
            DockerServiceTool::class.java,
            "node initializer",
        )
    }
}