package io.github.inductiveautomation.kindling.docker.services.model

import io.github.inductiveautomation.kindling.docker.services.ignition.model.ConnectionDefinition
import io.github.inductiveautomation.kindling.docker.services.ignition.model.IgnitionStaticDefinition

typealias EnvironmentVariable = Pair<String, String>

interface DockerEnvironmentVariableDefinition {
    val minimumVersion: String
    val default: String
    val options: List<String>?

    companion object {
        val variableDefinitionsByName = IgnitionStaticDefinition.entries.associateBy(Enum<*>::name)
        private val connectionVariableRegex = """GATEWAY_NETWORK_(?<i>\d+)""".toRegex()

        fun EnvironmentVariable.isConnectionVariable(): Boolean {
            return connectionVariableRegex.containsMatchIn(this.first)
        }

        fun String.getConnectionVariableFromInstance(): String? {
            val num = connectionVariableRegex.find(this)?.groups?.get("i")?.value ?: return null
            return replace(num, "X")
        }

        fun String.getConnectionVariableIndex(): Int? {
            return connectionVariableRegex.find(this)?.groups?.get("i")?.value?.toInt()
        }

        fun EnvironmentVariable.isDefaultOrEmpty(): Boolean {
            return if (isConnectionVariable()) {
                val name = first.getConnectionVariableFromInstance() ?: error("Invalid name: $first")
                ConnectionDefinition.valueOf(name).default == second
            } else {
                variableDefinitionsByName[first]?.default?.equals(second) ?: true
            }
        }

        fun EnvironmentVariable.toYamlString(): String {
            return "$first=$second"
        }

        fun createConnectionVariable(variable: ConnectionDefinition, index: Int, value: String? = null): EnvironmentVariable {
            val name = variable.name.replaceFirst("X", "$index")
            return EnvironmentVariable(name, value ?: variable.default)
        }

        fun MutableMap<String, String>.addOrRemove(environmentVariable: EnvironmentVariable) {
            if (environmentVariable.isDefaultOrEmpty()) {
                remove(environmentVariable.first)
            } else {
                put(environmentVariable.first, environmentVariable.second)
            }
        }
    }
}