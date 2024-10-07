package io.github.inductiveautomation.kindling.alarm

import com.inductiveautomation.ignition.common.alarming.AlarmEvent
import io.github.inductiveautomation.kindling.alarm.model.PersistedAlarmInfo
import io.github.inductiveautomation.kindling.cache.AliasingObjectInputStream
import io.github.inductiveautomation.kindling.core.ToolOpeningException
import io.github.inductiveautomation.kindling.core.ToolPanel
import java.nio.file.Path
import kotlin.io.path.inputStream

class AlarmCacheView(path: Path) : ToolPanel() {
    private val events: List<AlarmEvent> = try {
        val info = AliasingObjectInputStream(path.inputStream()) {
            put(
                "com.inductiveautomation.ignition.gateway.alarming.status.AlarmStateModel\$PersistedAlarmInfo",
                PersistedAlarmInfo::class.java,
            )
        }.readObject() as PersistedAlarmInfo

        info.data.values.flatMap { it.toList() }
    } catch (e: Exception) {
        throw ToolOpeningException("Error deserializing alarm cache. Only caches from Ignition 8.1.20+ are supported.", e)
    }
}
