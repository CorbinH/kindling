package io.github.inductiveautomation.kindling.docker.model.compose

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Deploy(
    @SerialName("mode")
    var mode: Mode = Mode.REPLICATED,
    @SerialName("replicas")
    var replicas: Int = 1,
    @SerialName("endpoint_mode")
    var endpointMode: EndpointMode = EndpointMode.VIP,
    @SerialName("labels")
    var labels: MutableMap<String, String> = mutableMapOf(),
    @SerialName("resources")
    var resources: Resources? = null,
    @SerialName("restart_policy")
    var restartPolicy: RestartPolicy = RestartPolicy(),
    @SerialName("placement")
    var placement: Placement = Placement(),
    @SerialName("update_config")
    var updateConfig: UpdateConfig = UpdateConfig(),
    @SerialName("rollback_config")
    var rollbackConfig: RollbackConfig = RollbackConfig()
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
        GLOBAL_JOB
    }

    @Serializable
    enum class EndpointMode {
        @SerialName("vip")
        VIP,
        @SerialName("dnsrr")
        DNSRR
    }

    @Serializable
    enum class Order {
        @SerialName("stop-first")
        STOP_FIRST,
        @SerialName("start-first")
        START_FIRST
    }

    @Serializable
    data class Resources(
        @SerialName("limits")
        var limits: Limits? = null,
        @SerialName("reservations")
        var reservations: Reservations? = null
    ) {
        @Serializable
        data class Limits(
            @SerialName("cpus")
            var cpus: String? = null,
            @SerialName("memory")
            var memory: String? = null,
            @SerialName("pids")
            var pids: Int? = null
        )

        @Serializable
        data class Reservations(
            @SerialName("cpus")
            var cpus: String? = null,
            @SerialName("memory")
            var memory: String? = null,
            @SerialName("generic_resources")
            var genericResources: MutableList<GenericResource> = mutableListOf()
        ) {
            @Serializable
            data class GenericResource(
                @SerialName("discrete_resource_spec")
                var discreteResourceSpec: DiscreteResourceSpec? = null
            ) {
                @Serializable
                data class DiscreteResourceSpec(
                    @SerialName("kind")
                    var kind: String,
                    @SerialName("value")
                    var value: Long
                )
            }
        }
    }

    @Serializable
    data class RestartPolicy(
        @SerialName("condition")
        var condition: Condition = Condition.ANY,
        @SerialName("delay")
        var delay: String = "0s",
        @SerialName("max_attempts")
        var maxAttempts: Int? = null,
        @SerialName("window")
        var window: String = "0s"
    ) {
        @Serializable
        enum class Condition {
            @SerialName("none")
            NONE,
            @SerialName("on-failure")
            ON_FAILURE,
            @SerialName("any")
            ANY
        }
    }

    @Serializable
    data class Placement(
        @SerialName("constraints")
        var constraints: MutableList<String> = mutableListOf(),
        @SerialName("preferences")
        var preferences: MutableList<Preference> = mutableListOf(),
        @SerialName("max_replicas_per_node")
        var maxReplicasPerNode: Int? = null
    ) {
        @Serializable
        data class Preference(
            @SerialName("spread")
            var spread: String
        )
    }

    @Serializable
    data class UpdateConfig(
        @SerialName("parallelism")
        var parallelism: Int = 1,
        @SerialName("delay")
        var delay: String = "0s",
        @SerialName("failure_action")
        var failureAction: FailureAction = FailureAction.PAUSE,
        @SerialName("monitor")
        var monitor: String = "0s",
        @SerialName("max_failure_ratio")
        var maxFailureRatio: Float = 0.0f,
        @SerialName("order")
        var order: Order = Order.STOP_FIRST
    ) {
        @Serializable
        enum class FailureAction {
            @SerialName("continue")
            CONTINUE,
            @SerialName("rollback")
            ROLLBACK,
            @SerialName("pause")
            PAUSE
        }
    }

    @Serializable
    data class RollbackConfig(
        @SerialName("parallelism")
        var parallelism: Int = 1,
        @SerialName("delay")
        var delay: String = "0s",
        @SerialName("failure_action")
        var failureAction: FailureAction = FailureAction.PAUSE,
        @SerialName("monitor")
        var monitor: String = "0s",
        @SerialName("max_failure_ratio")
        var maxFailureRatio: Float = 0.0f,
        @SerialName("order")
        var order: Order = Order.STOP_FIRST
    ) {
        @Serializable
        enum class FailureAction {
            @SerialName("continue")
            CONTINUE,
            @SerialName("pause")
            PAUSE
        }
    }
}
