package io.github.inductiveautomation.kindling.docker.serializers

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object MapOrListSerializer : KSerializer<MutableMap<String, String>> {
    private val listDelegate = ListSerializer(String.serializer())
    private val mapDelegate = MapSerializer(String.serializer(), String.serializer())

    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("MapOrList", SerialKind.CONTEXTUAL) {
        element("list", listDelegate.descriptor)
        element("map", mapDelegate.descriptor)
    }

    @Suppress("unchecked_cast")
    override fun deserialize(decoder: Decoder): MutableMap<String, String> {
        val value = try {
            decoder.decodeSerializableValue(mapDelegate)
        } catch (_: Exception) {
            decoder.decodeSerializableValue(listDelegate)
        }

        return if (value is Map<*, *>) {
            (value as Map<String, String>).toMutableMap()
        } else {
            (value as List<String>).associate {
                val (k, v) = it.split("=")
                k to v
            }.toMutableMap()
        }
    }

    override fun serialize(encoder: Encoder, value: MutableMap<String, String>) {
        encoder.encodeSerializableValue(mapDelegate, value)
    }
}
