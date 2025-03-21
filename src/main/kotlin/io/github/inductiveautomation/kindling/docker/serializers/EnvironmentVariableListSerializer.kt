package io.github.inductiveautomation.kindling.docker.serializers

import io.github.inductiveautomation.kindling.docker.model.EnvironmentVariable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object EnvironmentVariableListSerializer : KSerializer<List<EnvironmentVariable>> {
    private val delegate = ListSerializer(EnvironmentVariableAsStringSerializer)
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(
        encoder: Encoder,
        value: List<EnvironmentVariable>,
    ) {
        encoder.encodeSerializableValue(delegate, value)
    }

    override fun deserialize(decoder: Decoder): List<EnvironmentVariable> {
        return decoder.decodeSerializableValue(delegate)
    }
}