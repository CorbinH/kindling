package io.github.inductiveautomation.kindling.docker.services.ignition.model

import io.github.inductiveautomation.kindling.docker.services.model.DockerEnvironmentVariableDefinition
import java.util.TimeZone

enum class IgnitionStaticDefinition : DockerEnvironmentVariableDefinition {
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
        override val default = ""
        override val minimumVersion = "8.1.17"
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
    IGNITION_ROOT_KEY_PASSWORD_FILE {
        override val default = ""
        override val minimumVersion = "8.3.0"
    },
    IGNITION_ROOT_KEY_PASSWORD {
        override val default = ""
        override val minimumVersion = "8.3.0"
    },
    ACCEPT_MODULE_CERTS {
        override val default = ""
        override val minimumVersion = "8.3.0"
    },
    ACCEPT_MODULE_LICENSES {
        override val default = ""
        override val minimumVersion = "8.3.0"
    },
    ;

    override val options: List<String>? = null
}

enum class ConnectionDefinition : DockerEnvironmentVariableDefinition {
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
        override val default = "1000"
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