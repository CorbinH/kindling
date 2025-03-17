package io.github.inductiveautomation.kindling.docker.serializers

import io.github.inductiveautomation.kindling.docker.model.BindMount
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DockerVolumeBindingSerializer : KSerializer<BindMount> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "DockerVolumeServiceBinding",
        PrimitiveKind.STRING,
    )

    override fun deserialize(decoder: Decoder): BindMount {
        val strValue = decoder.decodeString()
        val name = strValue.substringBefore(":")
        val path = strValue.substringAfter(":").ifEmpty(fun(): String?  { return null })

        return BindMount(name, path)
    }

    override fun serialize(encoder: Encoder, value: BindMount) {
        val strValue = "${value.bindPath}:${value.containerPath}"
        encoder.encodeString(strValue)
    }
}