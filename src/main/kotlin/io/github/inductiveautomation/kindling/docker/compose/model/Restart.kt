package io.github.inductiveautomation.kindling.docker.compose.model

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

    @SerialName("unless-stopped")
    UNLESS_STOPPED,
}
