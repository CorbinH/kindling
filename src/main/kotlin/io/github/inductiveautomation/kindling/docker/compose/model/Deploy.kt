package io.github.inductiveautomation.kindling.docker.compose.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Deploy(
    var mode: Mode = Mode.REPLICATED,
    var replicas: Int = 1,
    @SerialName("endpoint_mode")
    var endpointMode: EndpointMode = EndpointMode.VIP,
    var labels: MutableMap<String, String> = mutableMapOf(),
    val resources: Resources = Resources(),
    @SerialName("restart_policy")
    var restartPolicy: RestartPolicy = RestartPolicy(),
    var placement: Placement = Placement(),
    @SerialName("update_config")
    var updateConfig: UpdateConfig = UpdateConfig(),
    @SerialName("rollback_config")
    var rollbackConfig: RollbackConfig = RollbackConfig(),
) {
    @Serializable
    enum class Mode {
        @SerialName("global")
        GLOBAL,

        @SerialName("replicated")
        REPLICATED,

        @SerialName("replicated-job")
        REPLICATED_JOB,

        @SerialName("global-job")
        GLOBAL_JOB,
    }

    @Serializable
    enum class EndpointMode {
        @SerialName("vip")
        VIP,

        @SerialName("dnsrr")
        DNSRR,
    }

    @Serializable
    enum class Order {
        @SerialName("stop-first")
        STOP_FIRST,

        @SerialName("start-first")
        START_FIRST,
    }

    @Serializable
    data class Resources(
        var limits: Limits = Limits(),
        var reservations: Reservations = Reservations(),
    ) {
        @Serializable
        data class Limits(
            var cpus: String? = null,
            var memory: String? = null,
            var pids: Int? = null,
        )

        @Serializable
        data class Reservations(
            var cpus: String? = null,
            var memory: String? = null,
        )
    }

    @Serializable
    data class RestartPolicy(
        var condition: Condition = Condition.ANY,
        var delay: String = "0s",
        @SerialName("max_attempts")
        var maxAttempts: Int? = null,
        var window: String = "0s",
    ) {
        @Serializable
        enum class Condition {
            @SerialName("none")
            NONE,

            @SerialName("on-failure")
            ON_FAILURE,

            @SerialName("any")
            ANY,
        }
    }

    @Serializable
    data class Placement(
        val constraints: MutableList<String> = mutableListOf(),
        val preferences: List<Preference> = listOf(Preference()),
        @SerialName("max_replicas_per_node")
        var maxReplicasPerNode: Int? = null,
    ) {
        @Serializable
        data class Preference(
            var spread: String? = null,
        )
    }

    @Serializable
    data class UpdateConfig(
        var parallelism: Int = 1,
        var delay: String = "0s",
        @SerialName("failure_action")
        var failureAction: FailureAction = FailureAction.PAUSE,
        var monitor: String = "0s",
        @SerialName("max_failure_ratio")
        var maxFailureRatio: Float = 0.0f,
        var order: Order = Order.STOP_FIRST,
    ) {
        @Serializable
        enum class FailureAction {
            @SerialName("continue")
            CONTINUE,

            @SerialName("rollback")
            ROLLBACK,

            @SerialName("pause")
            PAUSE,
        }
    }

    @Serializable
    data class RollbackConfig(
        var parallelism: Int = 1,
        var delay: String = "0s",
        @SerialName("failure_action")
        var failureAction: FailureAction = FailureAction.PAUSE,
        var monitor: String = "0s",
        @SerialName("max_failure_ratio")
        var maxFailureRatio: Float = 0.0f,
        @SerialName("order")
        var order: Order = Order.STOP_FIRST,
    ) {
        @Serializable
        enum class FailureAction {
            @SerialName("continue")
            CONTINUE,

            @SerialName("pause")
            PAUSE,
        }
    }
}