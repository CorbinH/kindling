package io.github.inductiveautomation.kindling.docker.model

typealias CliArgument = String

internal val CLI_REGEX = """(?:-\S )?\S+""".toRegex()

fun CliArgument.isValid(): Boolean {
    return CLI_REGEX.matches(this)
}

@Suppress("unused")
enum class GatewayCommandLineArgument {
    GATEWAY_NAME {
        override val displayName = "Gateway Name"
        override val flag = "-n"
    },
    PUBLIC_WEB_ADDRESS {
        override val displayName = "Public Web Address"
        override val flag = "-a"
    },
    PUBLIC_HTTP_PORT {
        override val displayName = "Public HTTP Port"
        override val flag = "-h"
    },
    PUBLIC_HTTPS_PORT {
        override val displayName = "Public HTTPS Port"
        override val flag = "-s"
    },
    MAX_JVM_MEMORY {
        override val displayName = "Max JVM Memory"
        override val flag = "-m"
    },
    GWBK_RESTORE_PATH {
        override val minimumVersion = "8.1.7"
        override val displayName = "GWBK Restore Path"
        override val flag = "-r"
    },
    DEBUG_MODE {
        override val flag = "-d"
        override val displayName = "Debug Mode"
    },
    ;

    abstract val displayName: String
    open val minimumVersion: String = "8.1.0"
    abstract val flag: String
}
