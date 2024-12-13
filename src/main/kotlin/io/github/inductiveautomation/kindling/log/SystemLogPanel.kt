package io.github.inductiveautomation.kindling.log

import io.github.inductiveautomation.kindling.utils.FileFilterSidebar
import io.github.inductiveautomation.kindling.utils.SQLiteConnection
import io.github.inductiveautomation.kindling.utils.executeQuery
import io.github.inductiveautomation.kindling.utils.get
import io.github.inductiveautomation.kindling.utils.toList
import io.github.inductiveautomation.kindling.utils.toMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.sql.Connection
import java.time.Instant

class SystemLogPanel(
    paths: List<Path>,
    fileData: List<LogFile<SystemLogEvent>>,
) : LogPanel<SystemLogEvent>(fileData.flatMap { it.items }, SystemLogColumns) {

    override val sidebar = FileFilterSidebar(
        listOf(
            LoggerNamePanel(rawData),
            LevelPanel(rawData),
            MDCPanel(rawData),
            ThreadPanel(rawData),
            TimePanel(rawData),
        ),
        fileData = paths.zip(fileData).toMap(),
    )

    init {
        filters.add { event ->
            val text = header.search.text
            if (text.isNullOrEmpty()) {
                true
            } else {
                text in event.message ||
                    event.logger.contains(text, ignoreCase = true) ||
                    event.thread.contains(text, ignoreCase = true) ||
                    event.stacktrace.any { stacktrace ->
                        stacktrace.contains(text, ignoreCase = true)
                    }
            }
        }

        addSidebar(sidebar)

        sidebar.forEach { filterPanel ->
            filterPanel.addFilterChangeListener {
                if (!sidebar.filterModelsAreAdjusting) updateData()
            }
        }

        if (paths.size > 1) {
            sidebar.addFileFilterChangeListener {
                selectedData = sidebar.selectedFiles.flatMap { it.items }
            }

            sidebar.registerHighlighters(table)
        }

        sidebar.configureFileDrop { files ->
            val newFileData = runBlocking {
                files.map { path ->
                    async(Dispatchers.IO) {
                        val connection = SQLiteConnection(path)
                        val logFile = LogFile(
                            connection.parseLogs().also { connection.close() },
                        )
                        path to logFile
                    }
                }.awaitAll()
            }

            rawData.addAll(
                newFileData.flatMap { it.second.items },
            )

            newFileData.toMap()
        }
    }

    companion object {
        fun Connection.parseLogs(): List<SystemLogEvent> {
            val stackTraces: Map<Long, List<String>> = executeQuery(
                """
                    SELECT
                        event_id,
                        trace_line
                    FROM 
                        logging_event_exception
                    ORDER BY
                        event_id,
                        i
                """.trimIndent(),
            )
                .toMap<Long, MutableList<String>> { rs ->
                    val key: Long = rs["event_id"]
                    val valueList = getOrPut(key, ::mutableListOf)
                    valueList.add(rs["trace_line"])
                }

            val mdcKeys: Map<Long, List<MDC>> = executeQuery(
                """
                    SELECT 
                        event_id,
                        mapped_key,
                        mapped_value
                    FROM 
                        logging_event_property
                    ORDER BY 
                        event_id
                """.trimIndent(),
            ).toMap<Long, MutableList<MDC>> { rs ->
                val key: Long = rs["event_id"]
                val valueList = getOrPut(key, ::mutableListOf)
                valueList +=
                    MDC(
                        rs["mapped_key"],
                        rs["mapped_value"],
                    )
            }

            return executeQuery(
                """
                    SELECT
                           event_id,
                           timestmp,
                           formatted_message,
                           logger_name,
                           level_string,
                           thread_name
                    FROM 
                        logging_event
                    ORDER BY
                        timestmp
                """.trimIndent(),
            ).toList { rs ->
                val eventId: Long = rs["event_id"]
                SystemLogEvent(
                    timestamp = Instant.ofEpochMilli(rs["timestmp"]),
                    message = rs["formatted_message"],
                    logger = rs["logger_name"],
                    thread = rs["thread_name"],
                    level = enumValueOf(rs["level_string"]),
                    mdc = mdcKeys[eventId].orEmpty(),
                    stacktrace = stackTraces[eventId].orEmpty(),
                )
            }
        }
    }
}