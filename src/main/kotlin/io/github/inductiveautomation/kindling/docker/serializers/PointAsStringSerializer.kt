package io.github.inductiveautomation.kindling.docker.serializers

import io.github.inductiveautomation.kindling.utils.getValue
import java.awt.Point
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object PointAsStringSerializer : KSerializer<Point> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(Point::class.java.name, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Point {
        val strValue = decoder.decodeString()
        val matchGroups = pointRegex.find(decoder.decodeString())?.groups ?: error("Invalid serialized format: $strValue")

        val x by matchGroups
        val y by matchGroups

        return Point(x.value.toInt(), y.value.toInt())
    }

    override fun serialize(encoder: Encoder, value: Point) {
        encoder.encodeString("(${value.x},${value.y})")
    }

    private val pointRegex = """\((?<x>-?\d+),(?<y>-?\d+)\)""".toRegex()
}