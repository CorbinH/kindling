package io.github.inductiveautomation.kindling.docker.serializers

import io.github.inductiveautomation.kindling.docker.DockerComposeFile
import io.github.inductiveautomation.kindling.docker.networks.model.DockerNetwork
import io.github.inductiveautomation.kindling.docker.services.model.DefaultDockerServiceModel
import io.github.inductiveautomation.kindling.docker.volumes.model.DockerVolume
import io.github.inductiveautomation.kindling.docker.services.ignition.model.IgnitionServiceModel
import io.github.inductiveautomation.kindling.docker.services.ignition.model.IgnitionServiceModel.Companion.asIgnitionServiceModelOrNull
import io.github.inductiveautomation.kindling.docker.services.mssql.model.MSSQLServiceModel
import io.github.inductiveautomation.kindling.docker.services.mssql.model.MSSQLServiceModel.Companion.asMSSQLServiceModelOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ComposeFileSerializer : KSerializer<DockerComposeFile> {
    private val delegate = DockerComposeFileDelegate.serializer()
    override val descriptor: SerialDescriptor get() = delegate.descriptor

    override fun deserialize(decoder: Decoder): DockerComposeFile {
        val fileDelegate = decoder.decodeSerializableValue(delegate)

        // Container names will be randomly generated, but we need to populate them from the keys of the map
        for ((containerName, node) in fileDelegate.services) {
            node.containerName = containerName
        }

        // Create instances of Networks/Volumes. This will change when more configuration is supported
        val volumes = fileDelegate.volumes.keys.map { DockerVolume(it) }

        return DockerComposeFile(
            fileDelegate.name,
            fileDelegate.services.values.map {
                it.asIgnitionServiceModelOrNull() ?: it.asMSSQLServiceModelOrNull() ?: it
            },
            volumes,
            fileDelegate.networks,
        )
    }

    override fun serialize(encoder: Encoder, value: DockerComposeFile) {
        val composeFile = DockerComposeFileDelegate(
            value.name,
            value.services.associate {
                when (it) {
                    is DefaultDockerServiceModel -> it.containerName to it
                    is IgnitionServiceModel -> it.containerName to it.model
                    is MSSQLServiceModel-> it.containerName to it.model
                    else -> error("Unknown model")
                }
            },
            value.volumes.associate { it.name to null },
            value.networks,
        )

        encoder.encodeSerializableValue(delegate, composeFile)
    }
}

@Serializable
internal class DockerComposeFileDelegate(
    val name: String? = null,
    val services: Map<String, DefaultDockerServiceModel> = emptyMap(),
    val volumes: Map<String, Nothing?> = emptyMap(),
    val networks: Map<String, DockerNetwork> = emptyMap(),
)
