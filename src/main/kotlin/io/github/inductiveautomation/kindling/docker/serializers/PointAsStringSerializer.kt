package io.github.inductiveautomation.kindling.docker.serializers

import io.github.inductiveautomation.kindling.utils.getValue
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.awt.Point

object PointAsStringSerializer : KSerializer<Point> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(Point::class.java.name, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Point {
        val str = decoder.decodeString()
        val groups = pointRegex.find(str)?.groups ?: throw SerializationException("Point is not of the format \"(x,y)\": $str")

        val x by groups
        val y by groups

        return try {
            Point(x.value.toInt(), y.value.toInt())
        } catch (e: NumberFormatException) {
            throw SerializationException("Unable to parse coordinates for point $str", e)
        }
    }

    override fun serialize(encoder: Encoder, value: Point) {
        encoder.encodeString("(${value.x},${value.y})")
    }

    private val pointRegex = """\((?<x>-?\d+),(?<y>-?\d+)\)""".toRegex()
}
