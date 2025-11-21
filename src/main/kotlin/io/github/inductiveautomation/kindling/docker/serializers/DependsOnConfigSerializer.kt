package io.github.inductiveautomation.kindling.docker.serializers

import io.github.inductiveautomation.kindling.docker.compose.model.DependsOn
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
import kotlin.reflect.jvm.jvmName

object DependsOnConfigSerializer : KSerializer<MutableMap<String, DependsOn>> {
    private val listDelegate = ListSerializer(String.serializer())
    private val mapDelegate = MapSerializer(String.serializer(), DependsOn.serializer())

    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor(DependsOn::class.jvmName, SerialKind.CONTEXTUAL) {
        element("list", listDelegate.descriptor)
        element("map", mapDelegate.descriptor)
    }

    override fun serialize(
        encoder: Encoder,
        value: MutableMap<String, DependsOn>,
    ) = mapDelegate.serialize(encoder, value)

    @Suppress("unchecked_cast")
    override fun deserialize(decoder: Decoder): MutableMap<String, DependsOn> {
        val value = try {
            decoder.decodeSerializableValue(listDelegate)
        } catch (_: Exception) {
            decoder.decodeSerializableValue(mapDelegate)
        }

        return if (value is Map<*, *>) {
            (value as Map<String, DependsOn>).toMutableMap()
        } else {
            (value as List<String>).associateWith { DependsOn() }.toMutableMap()
        }
    }
}
