package io.github.inductiveautomation.kindling.docker.serializers

import io.github.inductiveautomation.kindling.docker.model.DockerNetwork
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DockerNetworkSerializer : KSerializer<DockerNetwork> {
    private val delegate = MapSerializer(String.serializer(), MapSerializer(String.serializer(), String.serializer()))

    override val descriptor: SerialDescriptor = SerialDescriptor("DockerNetwork", delegate.descriptor)

    override fun deserialize(decoder: Decoder): DockerNetwork {
        val map = decoder.decodeSerializableValue(delegate)

        return DockerNetwork(
            name = map.keys.first()
        )
    }

    override fun serialize(encoder: Encoder, value: DockerNetwork) {
        val toSerialize = mapOf<String, Map<String, String>>(value.name to emptyMap())

        encoder.encodeSerializableValue(delegate, toSerialize)
    }
}