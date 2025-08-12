package io.github.inductiveautomation.kindling.docker.model

class MSSQLServiceModel(
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
        private val DEFAULT_VARIABLES = mapOf(
            MSSQLStaticDefinition.ACCEPT_EULA.name to "Y",
            MSSQLStaticDefinition.MSSQL_PID.name to "Developer",
        )

        fun DockerServiceModel.toMSSQLServiceModel(): MSSQLServiceModel {
            require(image.startsWith("kcollins/mssql")) {
                "Invalid image name for MSSQL Server: $image"
            }

            return MSSQLServiceModel(image, hostName, containerName, ports, environment, commands, volumes, networks, labels).apply {
                canvasLocation = this@toMSSQLServiceModel.canvasLocation
            }
        }

        fun DockerServiceModel.toMSSQLServiceModelOrNull(): MSSQLServiceModel? {
            return runCatching { toMSSQLServiceModel() }.getOrNull()
        }
    }
}
