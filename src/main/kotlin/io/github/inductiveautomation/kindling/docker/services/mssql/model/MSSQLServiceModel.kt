package io.github.inductiveautomation.kindling.docker.services.mssql.model

import io.github.inductiveautomation.kindling.docker.services.model.DefaultDockerServiceModel
import io.github.inductiveautomation.kindling.docker.services.model.DockerServiceModel

class MSSQLServiceModel(
    model: DefaultDockerServiceModel,
) : DockerServiceModel by model {

    init {
        if (this.hostName == null) {
            this.hostName = containerName.filter { it in hostNameValidChars }
        }
        this.environment.putAll(MSSQL_DEFAULT_VARIABLES)
    }

    var version: String = image.substringAfter(":")
        set(value) {
            field = value
            updateImage()
        }

    private fun updateImage() {
        image = "kcollins/mssql:$version"
    }

    companion object {
        const val DEFAULT_MSSQL_IMAGE = "kcollins/mssql:latest"
        private val MSSQL_DEFAULT_VARIABLES = mapOf(
            MSSQLStaticDefinition.ACCEPT_EULA.name to "Y",
            MSSQLStaticDefinition.MSSQL_DATABASE.name to "",
            MSSQLStaticDefinition.MSSQL_USER.name to "",
            MSSQLStaticDefinition.MSSQL_PID.name to "Developer",
            MSSQLStaticDefinition.MSSQL_SA_PASSWORD.name to "password",
        )
        private val hostNameValidChars = ('A'..'Z') + ('0'..'9') + ('a'..'z') + '-'
    }
}
