package io.github.inductiveautomation.kindling.docker.serializers

import io.github.inductiveautomation.kindling.docker.model.DockerVolumeServiceBinding
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DockerVolumeBindingSerializer : KSerializer<DockerVolumeServiceBinding> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "DockerVolumeServiceBinding",
        PrimitiveKind.STRING,
    )

    override fun deserialize(decoder: Decoder): DockerVolumeServiceBinding {
        val strValue = decoder.decodeString()
        val name = strValue.substringBefore(":")
        val path = strValue.substringAfter(":").ifEmpty(fun(): String?  { return null })

        return DockerVolumeServiceBinding(name, path)
    }

    override fun serialize(encoder: Encoder, value: DockerVolumeServiceBinding) {
        val strValue = "${value.volumeName}:${value.bindMount}"
        encoder.encodeString(strValue)
    }
}