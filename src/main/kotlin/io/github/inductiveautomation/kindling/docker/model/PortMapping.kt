package io.github.inductiveautomation.kindling.docker.model

import io.github.inductiveautomation.kindling.docker.serializers.PortMappingAsStringSerializer
import kotlinx.serialization.Serializable

@Serializable(with=PortMappingAsStringSerializer::class)
data class PortMapping(
    val hostPort: UShort,
    val containerPort: UShort,
)
