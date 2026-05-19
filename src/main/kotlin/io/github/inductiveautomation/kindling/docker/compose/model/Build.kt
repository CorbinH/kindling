package io.github.inductiveautomation.kindling.docker.compose.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Build(
    var context: String = ".",
    var dockerfile: String? = null,
    @SerialName("dockerfile_inline")
    var dockerfileInline: String? = null,
    val args: MutableMap<String, String> = mutableMapOf(),
    val ssh: MutableMap<String, String> = mutableMapOf(),
    @SerialName("cache_from")
    val cacheFrom: MutableList<String> = mutableListOf(),
    @SerialName("cache_to")
    val cacheTo: MutableList<String> = mutableListOf(),
    @SerialName("extra_hosts")
    val extraHosts: MutableList<String> = mutableListOf(),
    var isolation: String? = null,
    var privileged: Boolean = false,
    val labels: MutableMap<String, String> = mutableMapOf(),
    @SerialName("no_cache")
    var noCache: Boolean = false,
    var pull: Boolean = false,
    val secrets: MutableList<String> = mutableListOf(),
    val tags: MutableList<String> = mutableListOf(),
    var target: String? = null,
    @SerialName("shm_size")
    var shmSize: String? = null,
    val ulimits: MutableMap<String, String> = mutableMapOf(),
    val platforms: MutableList<String> = mutableListOf(),
)
