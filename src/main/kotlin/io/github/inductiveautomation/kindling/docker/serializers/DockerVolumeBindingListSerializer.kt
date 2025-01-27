package io.github.inductiveautomation.kindling.docker.serializers

import io.github.inductiveautomation.kindling.docker.model.DockerVolume
import io.github.inductiveautomation.kindling.docker.model.DockerVolumeServiceBinding
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class DockerVolumeBindingListSerializer : KSerializer<List<DockerVolumeServiceBinding>> {
    private val delegate = ListSerializer(String.serializer())

    override val descriptor: SerialDescriptor = SerialDescriptor(
        "DockerVolumeBinding",
        delegate.descriptor,
    )

    override fun deserialize(decoder: Decoder): List<DockerVolumeServiceBinding> {
        val strings = decoder.decodeSerializableValue(delegate)

        return strings.map {
            val (name, bindPath) = it.split(":")
            DockerVolumeServiceBinding(
                DockerVolume(name),
                bindPath.ifEmpty { null },
            )
        }.toMutableList()
    }

    override fun serialize(encoder: Encoder, value: List<DockerVolumeServiceBinding>) {
        val strings = value.map {
            "${it.volume.name}:${it.bindMount ?: ""}"
        }

        encoder.encodeSerializableValue(delegate, strings)
    }
}