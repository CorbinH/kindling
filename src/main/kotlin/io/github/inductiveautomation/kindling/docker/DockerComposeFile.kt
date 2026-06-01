package io.github.inductiveautomation.kindling.docker

import io.github.inductiveautomation.kindling.docker.networks.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.serializers.ComposeFileSerializer
import io.github.inductiveautomation.kindling.docker.services.model.DefaultDockerServiceModel
import io.github.inductiveautomation.kindling.docker.volumes.model.DockerVolume
import kotlinx.serialization.Serializable

@Serializable(with = ComposeFileSerializer::class)
data class DockerComposeFile(
    val name: String? = null,
    val services: List<DefaultDockerServiceModel>,
    val volumes: List<DockerVolume>,
    val networks: Map<String, DockerNetwork>,
) {
    fun isEmpty(): Boolean = services.isEmpty() && networks.isEmpty() && volumes.isEmpty()
}
