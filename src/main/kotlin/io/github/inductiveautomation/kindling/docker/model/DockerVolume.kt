package io.github.inductiveautomation.kindling.docker.model

import io.github.inductiveautomation.kindling.docker.serializers.DockerVolumeBindingSerializer
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

@Serializable(with = DockerVolumeBindingSerializer::class)
data class BindMount(
    var bindPath: String,
    var containerPath: String? = null,
)
