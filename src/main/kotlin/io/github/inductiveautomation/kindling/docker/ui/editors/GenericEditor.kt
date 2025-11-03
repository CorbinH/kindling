package io.github.inductiveautomation.kindling.docker.ui.editors

import io.github.inductiveautomation.kindling.docker.model.DockerServiceModel
import io.github.inductiveautomation.kindling.docker.ui.ConfigSection
import io.github.inductiveautomation.kindling.docker.ui.compose.ComposeEditor
import io.github.inductiveautomation.kindling.docker.ui.compose.RootEditor
import io.github.inductiveautomation.kindling.utils.FlatScrollPane
import io.github.inductiveautomation.kindling.utils.HorizontalSplitPane
import io.github.inductiveautomation.kindling.utils.treeCellRenderer
import net.miginfocom.swing.MigLayout
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.SwingUtilities

class GenericEditor(model: DockerServiceModel) : ConfigSection("Generic Compose Properties") {
    private val rootNode = RootEditor(model).apply {
        addValueChangeListener {
            fireConfigChange()
        }
    }

    private val tree = JTree(rootNode).apply {
        isRootVisible = false
        showsRootHandles = true

        cellRenderer = treeCellRenderer { _, value, _, _, _, _, _ ->
            text = (value as? ComposeEditor)?.name
            this@treeCellRenderer
        }

        addTreeSelectionListener {
            val comp = it.path.lastPathComponent as ComposeEditor
            editorArea.removeAll()
            editorArea.add(comp.component, "push, grow, span")
            SwingUtilities.invokeLater {
                editorArea.revalidate()
                editorArea.repaint()
            }
        }
    }

    private val editorArea = JPanel(MigLayout("fill, ins 4"))

    init {
        add(
            HorizontalSplitPane(
                FlatScrollPane(tree),
                FlatScrollPane(editorArea),
                0.2,
            ),
            "push, grow, span",
        )
    }
}
