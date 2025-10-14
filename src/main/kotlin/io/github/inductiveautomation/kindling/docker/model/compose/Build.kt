package io.github.inductiveautomation.kindling.docker.model.compose

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Build(
    var context: String,
    var dockerfile: String = "Dockerfile",
    @SerialName("dockerfile_inline")
    var dockerfileInline: String? = null,
    var args: MutableMap<String, String> = mutableMapOf(),
    var ssh: MutableMap<String, String> = mutableMapOf(),
    var cache: MutableList<Cache> = mutableListOf(),
    @SerialName("cache_from")
    var cacheFrom: MutableList<String> = mutableListOf(),
    @SerialName("cache_to")
    var cacheTo: MutableList<String> = mutableListOf(),
    @SerialName("extra_hosts")
    var extraHosts: MutableList<String> = mutableListOf(),
    var isolation: Isolation? = null,
    var privileged: Boolean = false,
    var labels: MutableMap<String, String> = mutableMapOf(),
    @SerialName("no_cache")
    var noCache: Boolean = false,
    var pull: Boolean = false,
    var secrets: MutableList<Secret> = mutableListOf(),
    var tags: MutableList<String> = mutableListOf(),
    var target: String? = null,
    @SerialName("shm_size")
    var shmSize: String? = null,
    var ulimits: MutableMap<String, Ulimit> = mutableMapOf(),
    var platforms: MutableList<String> = mutableListOf()
) {
    @Serializable
    enum class Isolation {
        @SerialName("default")
        DEFAULT,
        @SerialName("process")
        PROCESS,
        @SerialName("hyperv")
        HYPERV
    }

    @Serializable
    data class Cache(
        var type: String,
        var source: String? = null,
        var target: String? = null,
        var mode: String? = null,
        var gid: Int? = null,
        var uid: Int? = null
    )

    @Serializable
    data class Secret(
        var id: String,
        var src: String? = null
    )

    @Serializable
    data class Ulimit(
        var hard: Int,
        var soft: Int
    )
}
