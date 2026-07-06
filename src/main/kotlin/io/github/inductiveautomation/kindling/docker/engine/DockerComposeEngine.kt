package io.github.inductiveautomation.kindling.docker.engine

interface DockerComposeEngine {
    fun start(onProgress: (PullProgress) -> Unit = {}): ComposeResult
    fun stop(): ComposeResult
    fun restart(): ComposeResult
    fun delete(): ComposeResult

    /** Reads the stack's run-state and the hash stamped on its containers in a single query. */
    fun getSnapshot(): StackSnapshot
}

/**
 * A point-in-time view of the compose stack.
 *
 * @param remoteHash the `io.github.kindling.yaml-hash` label stamped on the running containers at
 *   start time, or null if no container carries one (e.g. no stack, or the engine is unreachable).
 */
data class StackSnapshot(
    val status: ComposeStatus,
    val remoteHash: String?,
)
