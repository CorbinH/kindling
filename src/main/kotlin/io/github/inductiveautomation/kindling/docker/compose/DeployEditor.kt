package io.github.inductiveautomation.kindling.docker.compose

import io.github.inductiveautomation.kindling.docker.compose.model.Deploy
import io.github.inductiveautomation.kindling.utils.DocumentAdapter
import io.github.inductiveautomation.kindling.utils.text
import net.miginfocom.swing.MigLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

@Suppress("unused")
class DeployEditor(data: Deploy) : ComposeObjectEditor<Deploy>("Deploy", data) {
    val mode by combo(options = Deploy.Mode.entries, initialValue = data.mode) {
        data.mode = it
    }

    val replicas by numeric(initialValue = data.replicas) { data.replicas = it ?: 1 }

    val endpointMode by combo("Endpoint Mode", Deploy.EndpointMode.entries, data.endpointMode) {
        data.endpointMode = it
    }

    val labels by map(value = data.labels)

    val resources by composeObject(ResourcesEditor(data.resources))

    val restartPolicy by composeObject(RestartPolicyEditor(data.restartPolicy))

    val placement by composeObject(PlacementEditor(data.placement))

    val updateConfig by composeObject(UpdateConfigEditor(data.updateConfig))

    val rollbackConfig by composeObject(RollbackConfigEditor(data.rollbackConfig))

    inner class ResourcesEditor(data: Deploy.Resources) : ComposeObjectEditor<Deploy.Resources>("Resources", data) {
        val limits by composeObject(LimitsEditor(data.limits))
        val reservations by composeObject(ReservationsEditor(data.reservations))

        inner class LimitsEditor(
            data: Deploy.Resources.Limits,
        ) : ComposeObjectEditor<Deploy.Resources.Limits>("Limits", data) {
            val cpus by text(value = data.cpus) { data.cpus = it }
            val memory by text(value = data.memory) { data.memory = it }
            val pids by numeric(initialValue = data.pids) { data.pids = it }
        }

        inner class ReservationsEditor(
            data: Deploy.Resources.Reservations,
        ) : ComposeObjectEditor<Deploy.Resources.Reservations>("Reservations", data) {
            val cpus by text(value = data.cpus) { data.cpus = it }
            val memory by text(value = data.memory) { data.memory = it }
        }
    }

    inner class RestartPolicyEditor(
        data: Deploy.RestartPolicy,
    ) : ComposeObjectEditor<Deploy.RestartPolicy>("Restart Policy", data) {
        val condition by combo(options = Deploy.RestartPolicy.Condition.entries, initialValue = data.condition) {
            data.condition = it
        }
        val delay by text(value = data.delay) { data.delay = it ?: "0s" }
        val maxAttempts by numeric("Max Attempts", data.maxAttempts) { data.maxAttempts = it }
        val window by text(value = data.window) { data.window = it ?: "0s" }
    }

    inner class PlacementEditor(
        data: Deploy.Placement,
    ) : ComposeObjectEditor<Deploy.Placement>("Placement", data) {
        val constraints by list(value = data.constraints) {}
        val preferences by composeValue {
            JPanel(MigLayout("fill, ins 0")).apply {
                val textField = JTextField(data.preferences.first().spread ?: "").apply {
                    document.addDocumentListener(
                        DocumentAdapter {
                            data.preferences.first().spread = it.document.text.ifBlank { null }
                            root?.fireValueChanged()
                        },
                    )
                }
                add(JLabel("spread:"))
                add(textField, "growx")
            }
        }
        val maxReplicasPerNode by numeric("Max Replicas per Node", data.maxReplicasPerNode) {
            data.maxReplicasPerNode = it
        }
    }

    inner class UpdateConfigEditor(
        data: Deploy.UpdateConfig,
    ) : ComposeObjectEditor<Deploy.UpdateConfig>("Update Config", data) {
        val parallelism by numeric(initialValue = data.parallelism) {
            data.parallelism = it ?: 1
        }
        val delay by text(value = data.delay) {
            data.delay = it ?: "0s"
        }
        val failureAction by combo("Failure Action", Deploy.UpdateConfig.FailureAction.entries, data.failureAction) {
            data.failureAction = it
        }
        val monitor by text(value = data.monitor) { data.monitor = it ?: "0s" }
        val maxFailureRatio by numeric(initialValue = data.maxFailureRatio) {
            data.maxFailureRatio = it ?: 0.0f
        }
        val order by combo(options = Deploy.Order.entries, initialValue = data.order) {
            data.order = it
        }
    }

    inner class RollbackConfigEditor(
        data: Deploy.RollbackConfig,
    ) : ComposeObjectEditor<Deploy.RollbackConfig>("Rollback Config", data) {
        val parallelism by numeric(initialValue = data.parallelism) {
            data.parallelism = it ?: 1
        }
        val delay by text(value = data.delay) {
            data.delay = it ?: "0s"
        }
        val failureAction by combo("Failure Action", Deploy.RollbackConfig.FailureAction.entries, data.failureAction) {
            data.failureAction = it
        }
        val monitor by text(value = data.monitor) { data.monitor = it ?: "0s" }
        val maxFailureRatio by numeric(initialValue = data.maxFailureRatio) {
            data.maxFailureRatio = it ?: 0.0f
        }
        val order by combo(options = Deploy.Order.entries, initialValue = data.order) {
            data.order = it
        }
    }
}
