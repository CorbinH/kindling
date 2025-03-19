package io.github.inductiveautomation.kindling.docker.serializers

import io.github.inductiveautomation.kindling.docker.model.EnvironmentVariable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object EnvironmentVariableSerializer : KSerializer<MutableMap<String, String>> {
    private val listDelegate = ListSerializer(EnvironmentVariableAsStringSerializer)
    private val mapDelegate = MapSerializer(String.serializer(), String.serializer())

    override val descriptor: SerialDescriptor = SerialDescriptor("EnvironmentVariable", mapDelegate.descriptor)

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
            (value as List<EnvironmentVariable>).toMap().toMutableMap()
        }
    }

    override fun serialize(encoder: Encoder, value: MutableMap<String, String>) {
        encoder.encodeSerializableValue(mapDelegate, value)
    }
}