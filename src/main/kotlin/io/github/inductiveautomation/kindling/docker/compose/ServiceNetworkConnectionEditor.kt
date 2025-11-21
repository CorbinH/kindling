package io.github.inductiveautomation.kindling.docker.compose

import io.github.inductiveautomation.kindling.docker.services.model.ServiceNetworkConnection
import io.github.inductiveautomation.kindling.utils.add
import io.github.inductiveautomation.kindling.utils.getAll
import javax.swing.event.EventListenerList

@Suppress("unused")
class ServiceNetworkConnectionEditor(
    data: ServiceNetworkConnection,
) : ComposeObjectEditor<ServiceNetworkConnection>("Network Connection", data), RootEditor {
    val ipv4Address by text("IPV4 Address", data.ipv4Address) { data.ipv4Address = it }
    val ipv6Address by text("IPV6 Address", data.ipv6Address) { data.ipv6Address = it }
    val linkLocalIps by list("Link Local IPs", data.linkLocalIPs)
    val macAddress by text("MAC Address", data.macAddress) { data.macAddress = it }
    val driverOpts by map("Driver Opts", data.driverOpts)
    val gwPriority by numeric("GW Priority", data.gwPriority) { data.gwPriority = it ?: 0 }
    val priority by numeric(initialValue = data.priority) { data.priority = it ?: 0 }

    private val listenerList = EventListenerList()

    override fun addValueChangeListener(l: RootEditor.ValueChangeListener) {
        listenerList.add(l)
    }

    override fun fireValueChanged() = listenerList.getAll<RootEditor.ValueChangeListener>().forEach {
        it.valueChange()
    }
}
