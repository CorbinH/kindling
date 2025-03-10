package io.github.inductiveautomation.kindling.docker.serializers

import io.github.inductiveautomation.kindling.docker.DockerComposeFile
import io.github.inductiveautomation.kindling.docker.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.model.DockerServiceModel
import io.github.inductiveautomation.kindling.docker.model.DockerVolume
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ComposeFileSerializer : KSerializer<DockerComposeFile> {
    private val delegate = DockerComposeFileDelegate.serializer()
    override val descriptor: SerialDescriptor get() = delegate.descriptor

    override fun deserialize(decoder: Decoder): DockerComposeFile {
        val fileDelegate = decoder.decodeSerializableValue(DockerComposeFileDelegate.serializer())

        // Container names will be randomly generated, but we need to populate them from the keys of the map
        for ((containerName, node) in fileDelegate.services) {
            node.containerName = containerName
        }

        // Create instances of Networks/Volumes. This will change when more configuration is supported
        val networks = fileDelegate.networks.keys.map { DockerNetwork(it) }
        val volumes = fileDelegate.volumes.keys.map { DockerVolume(it) }

        return DockerComposeFile(fileDelegate.services.values.toList(), volumes, networks)
    }

    override fun serialize(encoder: Encoder, value: DockerComposeFile) {
        val delegate = DockerComposeFileDelegate(
            value.services.associateBy { it.containerName },
            value.volumes.associate { it.name to null },
            value.networks.associate { it.name to null },
        )

        encoder.encodeSerializableValue(DockerComposeFileDelegate.serializer(), delegate)
    }
}

@Serializable
internal class DockerComposeFileDelegate(
    val services: Map<String, DockerServiceModel> = emptyMap(),
    val volumes: Map<String, Nothing?> = emptyMap(),
    val networks: Map<String, Nothing?> = emptyMap(),
)
