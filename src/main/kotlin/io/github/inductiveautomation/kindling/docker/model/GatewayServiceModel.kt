package io.github.inductiveautomation.kindling.docker.model

class GatewayServiceModel(
    image: String,
    hostName: String? = null,
    containerName: String,
    ports: MutableList<PortMapping> = mutableListOf(),
    environment: MutableMap<String, String> = mutableMapOf(),
    commands: MutableList<CliArgument> = mutableListOf(),
    volumes: MutableList<BindMount> = mutableListOf(),
    networks: MutableList<String> = mutableListOf(),
    labels: List<String> = emptyList(),
) : DockerServiceModel(image, hostName, containerName, ports, environment, commands, volumes, networks, labels) {

    init {
        if (this.hostName == null) {
            this.hostName = containerName.filter { it in hostNameValidChars }
        }

        this.environment.putAll(DEFAULT_VARIABLES)
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
        const val DEFAULT_IMAGE = "inductiveautomation/ignition:latest"
        private val hostNameValidChars = ('A'..'Z') + ('0'..'9') + ('a'..'z') + '-'
        private val DEFAULT_VARIABLES = mapOf(
            StaticDefinition.ACCEPT_IGNITION_EULA.name to "Y",
            StaticDefinition.GATEWAY_ADMIN_USERNAME.name to "admin",
            StaticDefinition.GATEWAY_ADMIN_PASSWORD.name to "password",
            StaticDefinition.IGNITION_EDITION.name to "standard",
        )

        fun DockerServiceModel.toGatewayServiceModel(): GatewayServiceModel {
            require(image.startsWith("inductiveautomation/ignition")) {
               "Invalid image name for Ignition gateway: $image"
            }

            return GatewayServiceModel(image, hostName, containerName, ports, environment,commands, volumes, networks, labels).apply {
                canvasLocation = this@toGatewayServiceModel.canvasLocation
            }
        }

        fun DockerServiceModel.toGatewayServiceModelOrNull(): GatewayServiceModel? {
            return runCatching { toGatewayServiceModel() }.getOrNull()
        }
    }
}