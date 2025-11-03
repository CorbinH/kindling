package io.github.inductiveautomation.kindling.docker.model.compose

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Restart {
    @SerialName("no")
    NO,

    @SerialName("always")
    ALWAYS,

    @SerialName("on-failure")
    ON_FAILURE,

    @SerialName("unless_stopped")
    UNLESS_STOPPED,
}
