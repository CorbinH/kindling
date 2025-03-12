package io.github.inductiveautomation.kindling.utils

import javax.swing.AbstractListModel
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

class KMutableListModel<T>(
    private val data: MutableList<T>,
) : AbstractListModel<T>() {
    fun add(element: T): Boolean {
        add(data.size, element)
        return true
    }

    fun add(index: Int, element: T) {
        data.add(index, element)
        fireIntervalAdded(this, index, index)
    }

    fun addAll(elements: Collection<T>): Boolean {
        return addAll(data.size, elements)
    }

    fun addAll(index: Int, elements: Collection<T>): Boolean {
        val added = data.addAll(index, elements)
        val endIndex = index + elements.size - 1
        if (added) fireIntervalAdded(this, index, endIndex)
        return added
    }

    fun clear() {
        val oldSize = data.size
        data.clear()
        fireIntervalRemoved(this, 0, oldSize - 1)
    }

    fun removeFirst(element: T): Boolean {
        val toRemove = data.indexOf(element)

        if (toRemove > -1) {
            val removed = data.remove(element)
            if (removed) {
                fireIntervalRemoved(this, toRemove, toRemove)
            }
            return removed
        }
        return false
    }

    fun removeAt(index: Int): T {
        val removed = data.removeAt(index)
        fireIntervalRemoved(this, index, index)
        return removed
    }

    fun removeAll(indices: Collection<Int>): Boolean {
        val descending = indices.filter { it > -1 }.sortedDescending().ifEmpty {
            return false
        }

        var startIndex = descending.first()
        var endIndex = startIndex

        for (i in 1..<descending.size) {
            val index = descending[i]

            if (index == startIndex - 1) {
                startIndex = index
                continue
            } else {
                endIndex.downTo(startIndex).forEach(data::removeAt)
                fireIntervalRemoved(this, startIndex, endIndex)
                startIndex = index
                endIndex = index
            }
        }

        endIndex.downTo(startIndex).forEach(data::removeAt)
        fireIntervalRemoved(this, startIndex, endIndex)
        return true
    }

    operator fun set(index: Int, element: T): T {
        val prev = data.set(index, element)
        fireContentsChanged(this, index, index)
        return prev
    }

    operator fun get(index: Int) = getElementAt(index)

    override fun getSize(): Int = data.size
    override fun getElementAt(index: Int): T = data[index]
}


/**
 * Allows passing a single function for listening to any list data events. (interval added, interval remove, interval changed)
 * Different logic can still be used for different event types by checking the `type` of the [ListDataEvent].
 *
 * @property anyChange the function to call for any list data change event.
 */
class TrivialListDataListener(
    private val anyChange: (e: ListDataEvent?) -> Unit = {},
): ListDataListener {
    override fun intervalAdded(e: ListDataEvent?) = anyChange(e)
    override fun intervalRemoved(e: ListDataEvent?) = anyChange(e)
    override fun contentsChanged(e: ListDataEvent?) = anyChange(e)
}

