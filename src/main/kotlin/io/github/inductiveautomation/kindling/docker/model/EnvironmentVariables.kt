package io.github.inductiveautomation.kindling.docker.model

import java.util.TimeZone

typealias EnvironmentVariable = Pair<String, String>

sealed interface GatewayEnvironmentVariableDefinition {
    val minimumVersion: String
    val default: String
    val options: List<String>?

    companion object {
        val variableDefinitionsByName = StaticDefinition.entries.associateBy(Enum<*>::name)
        val connectionVariableDefinitionsByName = ConnectionDefinition.entries.associateBy(Enum<*>::name)
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
                connectionVariableDefinitionsByName[name]?.default?.equals(second) ?: true
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

enum class StaticDefinition : GatewayEnvironmentVariableDefinition {
    TZ {
        override val minimumVersion = "8.0.0"
        override val default = "America/Los_Angeles"
        override val options = TimeZone.getAvailableIDs().toList()
    },
    ACCEPT_IGNITION_EULA {
        override val minimumVersion = "8.1.7"
        override val options = listOf("Y", "N")
        override val default = "N"
    },
    GATEWAY_RESTORE_DISABLED {
        override val default = "false"
        override val minimumVersion = "8.1.7"
        override val options = listOf("true", "false")
    },
    GATEWAY_ADMIN_USERNAME {
        override val default = "admin"
        override val minimumVersion = "8.1.8"
    },
    GATEWAY_ADMIN_PASSWORD {
        override val default = "password"
        override val minimumVersion = "8.1.8"
    },
    GATEWAY_HTTP_PORT {
        override val default = "8088"
        override val minimumVersion = "8.1.8"
    },
    GATEWAY_HTTPS_PORT {
        override val default = "8043"
        override val minimumVersion = "8.1.8"
    },
    GATEWAY_GAN_PORT {
        override val default = "8060"
        override val minimumVersion = "8.1.8"
    },
    IGNITION_EDITION {
        override val default = "standard"
        override val minimumVersion = "8.1.8"
        override val options = listOf("standard", "edge", "maker")
    },
    IGNITION_LICENSE_KEY {
        override val default = ""
        override val minimumVersion = "8.1.8"
    },
    IGNITION_ACTIVATION_TOKEN {
        override val default = ""
        override val minimumVersion = "8.1.8"
    },
    EAM_SETUP_INSTALLSELECTION {
        override val default = "Agent"
        override val minimumVersion = "8.1.10"
        override val options = listOf("Agent", "Controller")
    },
    EAM_AGENT_CONTROLLERSERVERNAME {
        override val default = ""
        override val minimumVersion = "8.1.10"
    },
    EAM_AGENT_SENDSTATSINTERVAL {
        override val default = "5"
        override val minimumVersion = "8.1.10"
    },
    EAM_CONTROLLER_ARCHIVEPATH {
        override val default = "data/eam_archive"
        override val minimumVersion = "8.1.10"
    },
    EAM_CONTROLLER_DATASOURCE {
        override val default = ""
        override val minimumVersion = "8.1.10"
    },
    EAM_CONTROLLER_ARCHIVELOCATION {
        override val default = "AUTOMATIC"
        override val minimumVersion = "8.1.10"
    },
    EAM_CONTROLLER_LOWDISKTHRESHOLDMB {
        override val default = ""
        override val minimumVersion = "8.1.10"
    },
    GATEWAY_MODULES_ENABLED {
        override val default = "true"
        override val minimumVersion = "8.1.17"
        override val options = listOf("true", "false")
    },
    IGNITION_UID {
        override val default = "2003"
        override val minimumVersion = "8.1.17"
    },
    IGNITION_GID {
        override val default = "TODO"
        override val minimumVersion = "8.1.17"
    },
    DISABLE_QUICKSTART {
        override val default = "false"
        override val minimumVersion = "8.1.23"
        override val options = listOf("true", "false")
    },
    GATEWAY_NETWORK_ENABLED {
        override val default = "true"
        override val minimumVersion = "8.1.32"
        override val options = listOf("true", "false")
    },
    GATEWAY_NETWORK_REQUIRESSL {
        override val default = "false"
        override val minimumVersion = "8.1.32"
        override val options = listOf("true", "false")
    },
    GATEWAY_NETWORK_REQUIRETWOWAYAUTH {
        override val default = "false"
        override val minimumVersion = "8.1.32"
        override val options = listOf("true", "false")
    },
    GATEWAY_NETWORK_SENDTHREADS {
        override val default = "5"
        override val minimumVersion = "8.1.32"
    },
    GATEWAY_NETWORK_RECEIVETHREADS {
        override val default = "5"
        override val minimumVersion = "8.1.32"
    },
    GATEWAY_NETWORK_RECEIVEMAX {
        override val default = ""
        override val minimumVersion = "8.1.32"
    },
    GATEWAY_NETWORK_ALLOWINCOMING {
        override val default = "true"
        override val minimumVersion = "8.1.32"
        override val options = listOf("true", "false")
    },
    GATEWAY_NETWORK_SECURITYPOLICY {
        override val default = "ApprovedOnly"
        override val minimumVersion = "8.1.32"
        override val options = listOf("ApprovedOnly", "Unrestricted", "SpecifiedList")
    },
    GATEWAY_NETWORK_WHITELIST {
        override val default = ""
        override val minimumVersion = "8.1.32"
    },
    GATEWAY_NETWORK_ALLOWEDPROXYHOPS {
        override val default = "0"
        override val minimumVersion = "8.1.32"
    },
    GATEWAY_NETWORK_WEBSOCKETSESSIONIDLETIMEOUT {
        override val default = ""
        override val minimumVersion = "8.1.32"
    },
    GATEWAY_ENCODING_KEY {
        override val default = ""
        override val minimumVersion = "8.1.38"
    },
    GATEWAY_ENCODING_KEY_FILE {
        override val default = ""
        override val minimumVersion = "8.1.38"
    },
    ;

    override val options: List<String>? = null
}

enum class ConnectionDefinition : GatewayEnvironmentVariableDefinition {
    // GAN Connection Environment Variables
    GATEWAY_NETWORK_X_HOST {
        override val default = ""
        override val minimumVersion = "8.1.10"
    },
    GATEWAY_NETWORK_X_PORT {
        override val default = "8060"
        override val minimumVersion = "8.1.10"
    },
    GATEWAY_NETWORK_X_PINGRATE {
        override val default =  "1000"
        override val minimumVersion = "8.1.10"
    },
    GATEWAY_NETWORK_X_PINGMAXMISSED {
        override val default = "30"
        override val minimumVersion = "8.1.10"
    },
    GATEWAY_NETWORK_X_ENABLED {
        override val default = "true"
        override val minimumVersion = "8.1.10"
    },
    GATEWAY_NETWORK_X_ENABLESSL {
        override val default = "true"
        override val minimumVersion = "8.1.10"
    },
    GATEWAY_NETWORK_X_WEBSOCKETTIMEOUT {
        override val default = "10000"
        override val minimumVersion = "8.1.10"
    },
    GATEWAY_NETWORK_X_DESCRIPTION {
        override val default = ""
        override val minimumVersion = "8.1.26"
    },
    ;
    override val options = null
}

object IgnitionVersionComparator : Comparator<String> {
    override fun compare(o1: String?, o2: String?): Int {
        if (o1 === o2) return 0
        if (o1 == null) return -1
        if (o2 == null) return 1

        // Nightly is greater than anything else
        if (o1.equals("NIGHTLY", true)) return 100
        if (o2.equals("NIGHTLY", true)) return -100

        // Latest is also greater than anything else
        if (o1.equals("LATEST", true)) return 99
        if (o2.equals("LATEST", true)) return -99


        val o1Split = o1.split(".", "-")
        val v1 = when(o1Split.size) {
            3 -> {
                val m = o1Split.map { it.toInt() }.toMutableList()
                m.add(1000)
                m
            }
            4 -> {
                o1Split.mapIndexed { index, value ->
                    if (index == 3) {
                        value.last().digitToInt()
                    } else {
                        value.toInt()
                    }
                }
            }
            else -> error("Malformed version: $o1")
        }

        val o2Split = o2.split(".", "-")
        val v2 = when(o2Split.size) {
            3 -> {
                val m = o2Split.map { it.toInt() }.toMutableList()
                m.add(1000) // Non release candidate is greater than release candidate
                m
            }
            4 -> {
                o2Split.mapIndexed { index, value ->
                    if (index == 3) {
                        value.last().digitToInt()
                    } else {
                        value.toInt()
                    }
                }
            }
            else -> error("Malformed version: $o1")
        }

        for ((version1, version2) in v1.zip(v2)) {
            val result = version1.compareTo(version2)
            if (result != 0) return result
        }

        return 0
    }
}
