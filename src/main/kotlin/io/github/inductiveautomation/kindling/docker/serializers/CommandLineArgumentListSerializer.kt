package io.github.inductiveautomation.kindling.docker.serializers

import io.github.inductiveautomation.kindling.docker.model.CLI_REGEX
import io.github.inductiveautomation.kindling.docker.model.CliArgument
import io.github.inductiveautomation.kindling.docker.model.isValid
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object CommandLineArgumentListSerializer : KSerializer<MutableSet<CliArgument>> {
    override fun deserialize(decoder: Decoder): MutableSet<CliArgument> {
        val set = mutableSetOf<CliArgument>()
        val strValue = decoder.decodeString()

        return CLI_REGEX.findAll(strValue).mapTo(set, MatchResult::value)
    }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "io.github.inductiveautomation.kindling.docker.serializers.CliSerializer",
        kind = PrimitiveKind.STRING,
    )

    override fun serialize(encoder: Encoder, value: MutableSet<CliArgument>) {
        for (arg in value) {
            if (!arg.isValid()) {
                error("Invalid command line argument: $arg")
            }
        }
        encoder.encodeString(value.joinToString(" "))
    }
}