package io.github.inductiveautomation.kindling.docker.engine

sealed class ComposeStatus {
    data object Running : ComposeStatus()
    data object Stopped : ComposeStatus()
    data class Partial(val running: Int, val total: Int) : ComposeStatus()
    data object Unknown : ComposeStatus()
}
