package io.github.inductiveautomation.kindling.docker.model.compose

import java.util.Enumeration
import javax.swing.tree.TreeNode
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class ComposeNode(private val properties: List<TreeNode>) : TreeNode {
}