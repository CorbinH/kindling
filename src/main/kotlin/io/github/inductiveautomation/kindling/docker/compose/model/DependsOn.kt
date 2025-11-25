package io.github.inductiveautomation.kindling.docker.compose.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DependsOn(
    var restart: Boolean = false,
    var condition: Condition = Condition.SERVICE_STARTED,
    var required: Boolean = true,
) {
    @Serializable
    enum class Condition {
        @SerialName("service_started")
        SERVICE_STARTED,

        @SerialName("service_healthy")
        SERVICE_HEALTHY,

        @SerialName("service_completed_successfully")
        SERVICE_COMPLETED_SUCCESSFULLY,
    }
}