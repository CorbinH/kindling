package io.github.paulgriffith.kindling.log

import java.time.Instant

sealed interface LogEvent {
    var marked: Boolean
    val timestamp: Instant
    val message: String
    val logger: String
    val level: Level?
    val stacktrace: List<String>
}

data class WrapperLogEvent(
    override var marked: Boolean,
    override val timestamp: Instant,
    override val message: String,
    override val logger: String = STDOUT,
    override val level: Level? = null,
    override val stacktrace: List<String> = emptyList()
) : LogEvent {
    companion object {
        const val STDOUT = "STDOUT"
    }
}

data class SystemLogsEvent(
    override var marked: Boolean,
    override val timestamp: Instant,
    override val message: String,
    override val logger: String,
    val thread: String,
    override val level: Level,
    val mdc: Map<String, String>,
    override val stacktrace: List<String>
) : LogEvent

@Suppress("unused")
enum class Level {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR;

    companion object {
        private val firstChars = values().associateBy { it.name.first() }

        fun valueOf(char: Char): Level = firstChars.getValue(char)
    }
}
