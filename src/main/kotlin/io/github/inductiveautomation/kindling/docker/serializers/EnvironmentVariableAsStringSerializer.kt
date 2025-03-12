package io.github.inductiveautomation.kindling.docker.serializers

import io.github.inductiveautomation.kindling.docker.model.EnvironmentVariable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object EnvironmentVariableAsStringSerializer : KSerializer<EnvironmentVariable> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "EnvironmentVariableAsString",
        PrimitiveKind.STRING,
    )

    override fun deserialize(decoder: Decoder): EnvironmentVariable {
        val strValue = decoder.decodeString()
        val (name, value) = strValue.split("=")
        return EnvironmentVariable(name, value)
    }

    override fun serialize(encoder: Encoder, value: EnvironmentVariable) {
        encoder.encodeString("${value.first}=${value.second}")
    }
}