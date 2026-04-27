package io.github.inductiveautomation.kindling.docker.engine

interface DockerComposeEngine {
    fun start(): ComposeResult
    fun stop(): ComposeResult
    fun restart(): ComposeResult
    fun delete(): ComposeResult
    fun getStatus(): ComposeStatus
    fun getRemoteHash(): String?
}
