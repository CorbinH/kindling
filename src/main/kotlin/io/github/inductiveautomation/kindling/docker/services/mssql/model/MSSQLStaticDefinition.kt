package io.github.inductiveautomation.kindling.docker.services.mssql.model

import io.github.inductiveautomation.kindling.docker.services.model.DockerEnvironmentVariableDefinition

enum class MSSQLStaticDefinition : DockerEnvironmentVariableDefinition {
    ACCEPT_EULA {
        override val minimumVersion = "2017"
        override val options = listOf("Y", "N")
        override val default = "Y"
    },
    MSSQL_DATABASE {
        override val default = ""
        override val options = null
        override val minimumVersion = "2017"
    },
    MSSQL_USER {
        override val default = ""
        override val options = null
        override val minimumVersion = "2017"
    },
    MSSQL_PID {
        override val default = "Developer"
        override val options = listOf("Developer", "Express", "Standard")
        override val minimumVersion = "2017"
    },
    MSSQL_SA_PASSWORD {
        override val default = "password"
        override val options = null;
        override val minimumVersion = "2017"
    }
}
