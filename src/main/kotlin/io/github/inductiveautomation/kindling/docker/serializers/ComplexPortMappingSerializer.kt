package io.github.inductiveautomation.kindling.docker.serializers

import io.github.inductiveautomation.kindling.docker.services.model.PortMapping
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ComplexPortMappingSerializer : KSerializer<PortMapping> {
    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor(
        PortMapping::class.java.name,
        SerialKind.CONTEXTUAL,
    ) {
        element(
            "simple",
            PrimitiveSerialDescriptor(
                PortMapping::class.java.name,
                PrimitiveKind.STRING,
            ),
        )
        element("complex", PortMapping.generatedSerializer().descriptor)
    }

    override fun deserialize(decoder: Decoder): PortMapping {
        try {
            val s = decoder.decodeString()
            val target = s.substringAfterLast(":")
            val published = s.substringBeforeLast(":")
            return PortMapping(published, target)
        } catch (_: Exception) {
            return decoder.decodeSerializableValue(PortMapping.generatedSerializer())
        }
    }

    override fun serialize(encoder: Encoder, value: PortMapping) {
        if (value.name == null) {
            encoder.encodeString("${value.published}:${value.target}")
        } else {
            encoder.encodeSerializableValue(PortMapping.generatedSerializer(), value)
        }
    }
}
