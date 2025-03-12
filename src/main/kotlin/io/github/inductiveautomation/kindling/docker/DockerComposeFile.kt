package io.github.inductiveautomation.kindling.docker

import io.github.inductiveautomation.kindling.docker.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.model.DockerServiceModel
import io.github.inductiveautomation.kindling.docker.model.DockerVolume
import io.github.inductiveautomation.kindling.docker.serializers.ComposeFileSerializer
import kotlinx.serialization.Serializable

@Serializable(with = ComposeFileSerializer::class)
data class DockerComposeFile(
    val services: List<DockerServiceModel>,
    val volumes: List<DockerVolume>,
    val networks: List<DockerNetwork>,
) {
    fun isEmpty(): Boolean {
        return services.isEmpty() && networks.isEmpty() && volumes.isEmpty()
    }
}