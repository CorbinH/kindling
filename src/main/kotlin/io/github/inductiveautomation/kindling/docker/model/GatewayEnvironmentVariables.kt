package io.github.inductiveautomation.kindling.docker.model

import com.jidesoft.comparator.AlphanumComparator
import java.util.TimeZone

@Suppress("unused")
enum class GatewayEnvironmentVariables {
    TZ {
        override val minimumSupportedVersion = "8.0.0"
        override val options: List<String>
            get() = TimeZone.getAvailableIDs().sortedWith(AlphanumComparator(false))
    },
    ACCEPT_IGNITION_EULA {
        override val minimumSupportedVersion = "8.1.7"
        override val options: List<String> = listOf("Y", "N")
    },
    GATEWAY_RESTORE_DISABLED {
        override val minimumSupportedVersion = "8.1.7"
        override val options: List<String> = listOf("true" , "false")
    },
    GATEWAY_ADMIN_USERNAME {
        override val minimumSupportedVersion = "8.1.8"
    },
    GATEWAY_ADMIN_PASSWORD {
        override val minimumSupportedVersion = "8.1.8"
    },
    GATEWAY_HTTP_PORT {
        override val minimumSupportedVersion = "8.1.8"
    },
    GATEWAY_HTTPS_PORT {
        override val minimumSupportedVersion = "8.1.8"
    },
    GATEWAY_GAN_PORT {
        override val minimumSupportedVersion = "8.1.8"
    },
    IGNITION_EDITION {
        override val minimumSupportedVersion = "8.1.8"
        override val options = listOf("standard", "edge", "maker")
    },
    IGNITION_LICENSE_KEY {
        override val minimumSupportedVersion = "8.1.8"
    },
    IGNITION_ACTIVATION_TOKEN {
        override val minimumSupportedVersion = "8.1.8"
    },
    // 8.1.10+
    GATEWAY_NETWORK_X_HOST {
        override val minimumSupportedVersion = "8.1.10"
    },
    GATEWAY_NETWORK_X_PORT {
        override val minimumSupportedVersion = "8.1.10"
    },
    GATEWAY_NETWORK_X_PINGRATE {
        override val minimumSupportedVersion = "8.1.10"
    },
    GATEWAY_NETWORK_X_PINGMAXMISSED {
        override val minimumSupportedVersion = "8.1.10"
    },
    GATEWAY_NETWORK_X_ENABLED {
        override val options = listOf("true", "false")
        override val minimumSupportedVersion = "8.1.10"
    },
    GATEWAY_NETWORK_X_ENABLESSL {
        override val options = listOf("true", "false")
        override val minimumSupportedVersion = "8.1.10"
    },
    GATEWAY_NETWORK_X_WEBSOCKETTIMEOUT {
        override val minimumSupportedVersion = "8.1.10"
    },
    EAM_SETUP_INSTALLSELECTION {
        override val options = listOf("Agent", "Controller")
        override val minimumSupportedVersion = "8.1.10"
    },
    EAM_AGENT_CONTROLLERSERVERNAME {
        override val minimumSupportedVersion = "8.1.10"
    },
    EAM_AGENT_SENDSTATSINTERVAL {
        override val minimumSupportedVersion = "8.1.10"
    },
    EAM_CONTROLLER_ARCHIVEPATH {
        override val minimumSupportedVersion = "8.1.10"
    },
    EAM_CONTROLLER_DATASOURCE {
        override val minimumSupportedVersion = "8.1.10"
    },
    EAM_CONTROLLER_ARCHIVELOCATION {
        override val minimumSupportedVersion = "8.1.10"
    },
    EAM_CONTROLLER_LOWDISKTHRESHOLDMB {
        override val minimumSupportedVersion = "8.1.10"
    },
    // 8.1.17+
    GATEWAY_MODULES_ENABLED {
        override val minimumSupportedVersion = "8.1.17"
    },
    IGNITION_UID {
        override val minimumSupportedVersion = "8.1.17"
    },
    IGNITION_GID {
        override val minimumSupportedVersion = "8.1.17"
    },
    // 8.1.23+
    DISABLE_QUICKSTART {
        override val options = listOf("true", "false")
        override val minimumSupportedVersion = "8.1.23"
    },
    // 8.1.26+
    GATEWAY_NETWORK_X_DESCRIPTION {
        override val minimumSupportedVersion = "8.1.26"
    },
    // 8.1.32+
    GATEWAY_NETWORK_ENABLED {
        override val options = listOf("true", "false")
        override val minimumSupportedVersion = "8.1.32"
    },
    GATEWAY_NETWORK_REQUIRESSL {
        override val options = listOf("true", "false")
        override val minimumSupportedVersion = "8.1.32"
    },
    GATEWAY_NETWORK_REQUIRETWOWAYAUTH {
        override val options = listOf("true", "false")
        override val minimumSupportedVersion = "8.1.32"
    },
    GATEWAY_NETWORK_SENDTHREADS {
        override val minimumSupportedVersion = "8.1.32"
    },
    GATEWAY_NETWORK_RECEIVETHREADS {
        override val minimumSupportedVersion = "8.1.32"
    },
    GATEWAY_NETWORK_RECEIVEMAX {
        override val minimumSupportedVersion = "8.1.32"
    },
    GATEWAY_NETWORK_ALLOWINCOMING {
        override val minimumSupportedVersion = "8.1.32"
        override val options = listOf("true", "false")
    },
    GATEWAY_NETWORK_SECURITYPOLICY {
        override val minimumSupportedVersion = "8.1.32"
        override val options = listOf("ApprovedOnly", "Unrestricted", "SpecifiedList")
    },
    GATEWAY_NETWORK_WHITELIST {
        override val minimumSupportedVersion = "8.1.32"
    },
    GATEWAY_NETWORK_ALLOWEDPROXYHOPS {
        override val minimumSupportedVersion = "8.1.32"
    },
    GATEWAY_NETWORK_WEBSOCKETSESSIONIDLETIMEOUT {
        override val minimumSupportedVersion = "8.1.32"
    },
    // 8.1.38+
    GATEWAY_ENCODING_KEY {
        override val minimumSupportedVersion = "8.1.38"
    },
    GATEWAY_ENCODING_KEY_FILE {
        override val minimumSupportedVersion = "8.1.38"
    },
    ;

    abstract val minimumSupportedVersion: String
    open val options: List<String>? = null
}

object IgnitionVersionComparator : Comparator<String> {
    override fun compare(o1: String?, o2: String?): Int {
        if (o1 === o2) return 0
        if (o1 == null) return -1
        if (o2 == null) return 1

        val v1 = o1.split(".").map { it.toInt() }
        val v2 = o2.split(".").map { it.toInt() }

        require(v1.size == 3) { "Invalid version: $o1" }
        require(v2.size == 3) { "Invalid version: $o2" }

        for ((version1, version2) in v1.zip(v2)) {
            val result = version1.compareTo(version2)
            if (result != 0) return result
        }

        return 0
    }
}
