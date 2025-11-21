package io.github.inductiveautomation.kindling.docker.services.mssql.model

import io.github.inductiveautomation.kindling.docker.services.model.DefaultDockerServiceModel
import io.github.inductiveautomation.kindling.docker.services.model.DockerServiceModel

class MSSQLServiceModel(
    val model: DefaultDockerServiceModel,
) : DockerServiceModel by model {

    init {
        if (this.hostName == null) {
            this.hostName = containerName.filter { it in hostNameValidChars }
        }
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
        private val hostNameValidChars = ('A'..'Z') + ('0'..'9') + ('a'..'z') + '-'

        fun DefaultDockerServiceModel.asMSSQLServiceModel(): MSSQLServiceModel {
            require(image.startsWith("kcollins/mssql")) {
                "Invalid image name for MSSQL Server: $image"
            }

            return MSSQLServiceModel(this)
        }

        fun DefaultDockerServiceModel.asMSSQLServiceModelOrNull(): MSSQLServiceModel? {
            return runCatching { asMSSQLServiceModel() }.getOrNull()
        }
    }
}