package io.github.inductiveautomation.kindling.docker.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DockerVolume(
    val name: String,
    var driver: String? = null,
    @SerialName("driver_opts")
    val driverOpts: MutableMap<String, String> = mutableMapOf(),
    var external: Boolean = false,
    val labels: MutableList<String> = mutableListOf(),
)

@Serializable
data class DockerVolumeServiceBinding(
    var volume: DockerVolume,
    var bindMount: String? = null,
)

val DockerVolumeServiceBinding.isBindMount: Boolean
    get() = bindMount.isNullOrEmpty()
