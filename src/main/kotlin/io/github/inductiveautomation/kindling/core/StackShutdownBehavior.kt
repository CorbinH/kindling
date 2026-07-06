package io.github.inductiveautomation.kindling.core

import kotlinx.serialization.Serializable

/** What to do with a running Docker Compose stack when Kindling is closed. */
@Serializable
enum class StackShutdownBehavior(val description: String) {
    LeaveRunning("Leave the stack running"),
    Stop("Stop the stack"),
    Delete("Delete the stack"),
}
