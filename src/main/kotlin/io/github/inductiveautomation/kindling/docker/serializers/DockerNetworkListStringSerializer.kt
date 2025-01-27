package io.github.inductiveautomation.kindling.docker.serializers

import io.github.inductiveautomation.kindling.docker.model.DockerNetwork
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DockerNetworkListStringSerializer : KSerializer<List<DockerNetwork>> {
    private val delegate = ListSerializer(String.serializer())

    override val descriptor: SerialDescriptor = SerialDescriptor("DOckerNetworkAsString", delegate.descriptor)

    override fun deserialize(decoder: Decoder): List<DockerNetwork> {
        val nameList = decoder.decodeSerializableValue(delegate)
        return nameList.map {
            DockerNetwork(name = it)
        }
    }

    override fun serialize(encoder: Encoder, value: List<DockerNetwork>) {
        val toSerialize = value.map { it.name }
        encoder.encodeSerializableValue(delegate, toSerialize)
    }
}