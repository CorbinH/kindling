package io.github.inductiveautomation.kindling.docker.services.ignition.model

import io.github.inductiveautomation.kindling.docker.services.model.DefaultDockerServiceModel
import io.github.inductiveautomation.kindling.docker.services.model.DockerServiceModel

class IgnitionServiceModel(
    model: DefaultDockerServiceModel,
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
        private val hostNameValidChars = ('A'..'Z') + ('0'..'9') + ('a'..'z') + '-'
        private val IGNITION_DEFAULT_VARIABLES = mapOf(
            IgnitionStaticDefinition.ACCEPT_IGNITION_EULA.name to "Y",
            IgnitionStaticDefinition.GATEWAY_ADMIN_USERNAME.name to "admin",
            IgnitionStaticDefinition.GATEWAY_ADMIN_PASSWORD.name to "password",
            IgnitionStaticDefinition.IGNITION_EDITION.name to "standard",
        )
    }
}
enum class GatewayServiceFlavor {
    KCOLLINS,
    INDUCTIVEAUTOMATION,
    ;

    val serialName: String = name.lowercase()
}

/** Container-side HTTP port the gateway listens on: the GATEWAY_HTTP_PORT env override, or 8088. */
val IgnitionServiceModel.gatewayHttpPort: String
    get() = environment[IgnitionStaticDefinition.GATEWAY_HTTP_PORT.name]
        ?.takeIf { it.isNotBlank() }
        ?: IgnitionStaticDefinition.GATEWAY_HTTP_PORT.default

/** Host port bound to the gateway's HTTP port, or null if it isn't published. */
val IgnitionServiceModel.gatewayHostPort: String?
    get() = ports.find { it.target == gatewayHttpPort }?.published
