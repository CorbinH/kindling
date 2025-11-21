package io.github.inductiveautomation.kindling.docker.services.ignition.model

import io.github.inductiveautomation.kindling.docker.services.model.DefaultDockerServiceModel
import io.github.inductiveautomation.kindling.docker.services.model.DockerServiceModel

class IgnitionServiceModel(
    val model: DefaultDockerServiceModel,
) : DockerServiceModel by model {

    init {
        if (this.hostName == null) {
            this.hostName = containerName.filter { it in hostNameValidChars }
        }

        this.environment.putAll(IGNITION_DEFAULT_VARIABLES)
    }

    var flavor: GatewayServiceFlavor = GatewayServiceFlavor.valueOf(image.substringBefore("/").uppercase())
        set(value) {
            field = value
            updateImage()
        }

    var version: String = image.substringAfter(":")
        set(value) {
            field = value
            updateImage()
        }

    private fun updateImage() {
        image = "${flavor.serialName}/ignition:$version"
    }

    companion object {
        const val IGNITION_DEFAULT_IMAGE = "inductiveautomation/ignition:latest"
        private val hostNameValidChars = ('A'..'Z') + ('0'..'9') + ('a'..'z') + '-'
        private val IGNITION_DEFAULT_VARIABLES = mapOf(
            IgnitionStaticDefinition.ACCEPT_IGNITION_EULA.name to "Y",
            IgnitionStaticDefinition.GATEWAY_ADMIN_USERNAME.name to "admin",
            IgnitionStaticDefinition.GATEWAY_ADMIN_PASSWORD.name to "password",
            IgnitionStaticDefinition.IGNITION_EDITION.name to "standard",
        )

        fun DefaultDockerServiceModel.asIgnitionServiceModel(): IgnitionServiceModel {
            require(image.startsWith("inductiveautomation/ignition")) {
                "Invalid image name for Ignition gateway: $image"
            }

            return IgnitionServiceModel(this)
        }

        fun DefaultDockerServiceModel.asIgnitionServiceModelOrNull(): IgnitionServiceModel? {
            return runCatching { asIgnitionServiceModel() }.getOrNull()
        }
    }
}
enum class GatewayServiceFlavor {
    KCOLLINS,
    INDUCTIVEAUTOMATION,
    ;

    val serialName: String = name.lowercase()
}
