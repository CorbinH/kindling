package io.github.inductiveautomation.kindling.docker.serializers

import io.github.inductiveautomation.kindling.docker.model.DockerServiceModel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DockerServiceModelSerializer : KSerializer<DockerServiceModel> {
    private val delegate = DockerServiceModel.generatedSerializer()
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(
        encoder: Encoder,
        value: DockerServiceModel,
    ) {
        value.envList = value.environment.toList()
        encoder.encodeSerializableValue(delegate, value)
    }

    override fun deserialize(decoder: Decoder): DockerServiceModel {
        return decoder.decodeSerializableValue(delegate)
    }
}