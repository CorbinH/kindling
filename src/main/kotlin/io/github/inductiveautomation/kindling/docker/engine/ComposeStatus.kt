package io.github.inductiveautomation.kindling.docker.engine

sealed class ComposeStatus {
    data object Running : ComposeStatus()

    /** Containers exist for this project, but none are running. */
    data object Stopped : ComposeStatus()
    data class Partial(val running: Int, val total: Int) : ComposeStatus()

    /** No containers exist for this project. */
    data object NoStack : ComposeStatus()

    /** The Docker engine could not be reached. */
    data object Unknown : ComposeStatus()
}
