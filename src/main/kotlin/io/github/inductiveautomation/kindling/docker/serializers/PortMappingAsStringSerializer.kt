package io.github.inductiveautomation.kindling.docker.serializers

import io.github.inductiveautomation.kindling.docker.model.PortMapping
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object PortMappingAsStringSerializer : KSerializer<PortMapping> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "io.github.inductiveautomation.kindling.docker.serializers.PortMappingAsStringSerializer",
        PrimitiveKind.STRING,
    )

    override fun deserialize(decoder: Decoder): PortMapping {
        val s = decoder.decodeString()

        val (host, container) = s.split(":")
        return PortMapping(host.toUShort(), container.toUShort())
    }

    override fun serialize(encoder: Encoder, value: PortMapping) {
        encoder.encodeString("${value.hostPort}:${value.containerPort}")
    }
}