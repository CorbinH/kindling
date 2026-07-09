package io.github.inductiveautomation.kindling.docker.engine

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.PullResponseItem
import com.github.dockerjava.api.model.Statistics
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.core.InvocationBuilder
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import io.github.inductiveautomation.kindling.docker.DockerComposeFile
import io.github.inductiveautomation.kindling.utils.getLogger
import io.github.inductiveautomation.kindling.utils.transferTo
import java.nio.file.Files
import java.nio.file.Path

class ProcessComposeEngine(
    private var projectName: String,
    private val composeProvider: () -> DockerComposeFile,
    private val serializer: (DockerComposeFile) -> String,
    private val workingDirProvider: () -> Path? = { null },
) : DockerComposeEngine {
    private lateinit var composeObject: DockerComposeFile

    // Previous (totalUsage, systemCpuUsage) per container name, so CPU% is a delta over the poll
    // interval rather than the daemon's cumulative-since-start figure.
    private val previousCpu = HashMap<String, Pair<Long, Long>>()

    private fun writeComposeFile(): Path {
        composeObject = composeProvider.invoke()
        // Docker resolves relative bind-mount paths against the compose file's parent folder, so the
        // temp file must live alongside the gwbks (the working dir), not in the system temp dir.
        val dir = workingDirProvider()
        val tempFile = if (dir != null) {
            runCatching { Files.createTempFile(dir, "kindling-compose-", ".yaml") }
                .getOrElse { Files.createTempFile("kindling-compose-", ".yaml") }
        } else {
            Files.createTempFile("kindling-compose-", ".yaml")
        }
        tempFile.toFile().outputStream().use { out ->
            serializer(composeObject).byteInputStream().use { file ->
                file transferTo out
            }
        }
        return tempFile
    }

    private fun compose(file: Path, vararg args: String): ComposeResult {
        // Images are pulled ahead of time via the Engine API (see pullImages), so `up` won't
        // re-download them; its output is small enough to capture directly for the result dialog.
        val process = ProcessBuilder("docker", "compose", "-f", file.toString(), "-p", projectName, *args)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return if (exitCode == 0) {
            LOGGER.info("docker compose ${args.joinToString(" ")}: $output")
            ComposeResult.Success(output)
        } else {
            LOGGER.error("docker compose ${args.joinToString(" ")} failed (exit $exitCode): $output")
            ComposeResult.Failure(exitCode, output)
        }
    }

    /**
     * Pulls every image referenced in the compose stack via the Engine API, reporting per-image
     * byte progress through [onProgress]. Pulling here (rather than letting `docker compose up`
     * do it) is what gives us download percentages: the CLI strips progress when it detects it
     * isn't attached to a TTY, but the Engine API always streams structured layer events.
     */
    private fun pullImages(onProgress: (PullProgress) -> Unit) {
        val images = composeObject.services
            .map { it.image.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        // exec(callback) kicks off the pull asynchronously and returns immediately, so starting
        // every pull before awaiting any lets them download in parallel; we then block until all
        // streams have completed. awaitCompletion() rethrows any pull failure (e.g. missing image).
        val pulls = images.mapIndexed { index, image ->
            image to startPull(image, index + 1, images.size, onProgress)
        }
        pulls.forEach { (image, callback) ->
            runCatching { callback.awaitCompletion() }
                .onFailure { LOGGER.warn("Failed to pull image $image: ${it.message}") }
        }
    }

    private fun startPull(
        image: String,
        index: Int,
        total: Int,
        onProgress: (PullProgress) -> Unit,
    ): ResultCallback.Adapter<PullResponseItem> {
        // The tag is whatever follows the last ':' that comes after the final '/', so registry
        // host:port prefixes (e.g. "registry:5000/foo") aren't mistaken for a tag.
        val lastSlash = image.lastIndexOf('/')
        val tagSeparator = image.indexOf(':', startIndex = lastSlash + 1)
        val repository = if (tagSeparator >= 0) image.substring(0, tagSeparator) else image
        val tag = if (tagSeparator >= 0) image.substring(tagSeparator + 1) else "latest"

        // Docker streams progress per layer; sum across layers for an overall figure.
        val downloadedByLayer = HashMap<String, Long>()
        val totalByLayer = HashMap<String, Long>()

        val callback = object : ResultCallback.Adapter<PullResponseItem>() {
            override fun onNext(item: PullResponseItem) {
                val id = item.id ?: return
                item.progressDetail?.let { detail ->
                    detail.current?.let { downloadedByLayer[id] = it }
                    detail.total?.let { if (it > 0) totalByLayer[id] = it }
                }
                onProgress(
                    PullProgress(
                        image = image,
                        imageIndex = index,
                        imageCount = total,
                        downloadedBytes = downloadedByLayer.values.sum(),
                        totalBytes = totalByLayer.values.sum(),
                        status = item.status.orEmpty(),
                    ),
                )
            }
        }

        return docker.pullImageCmd(repository).withTag(tag).exec(callback)
    }

    // For commands that operate on the running stack by project name — no compose file needed
    private fun composeByProject(vararg args: String): ComposeResult {
        val process = ProcessBuilder("docker", "compose", "-p", projectName, *args)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        return if (exitCode == 0) {
            LOGGER.info("docker compose ${args.joinToString(" ")}: $output")
            ComposeResult.Success(output)
        } else {
            LOGGER.error("docker compose ${args.joinToString(" ")} failed (exit $exitCode): $output")
            ComposeResult.Failure(exitCode, output)
        }
    }

    private fun removeConflictingContainers(composeFile: Path) {
        val names = composeFile.toFile().readLines()
            .map { it.trim() }
            .filter { it.startsWith("container_name:") }
            .map { it.removePrefix("container_name:").trim() }
            .filter { it.isNotBlank() }

        if (names.isEmpty()) return

        for (name in names) {
            // Returns empty list rather than hanging/throwing if Docker is unavailable
            val containers = runCatching {
                docker.listContainersCmd()
                    .withNameFilter(listOf(name))
                    .withShowAll(true)
                    .exec()
                    .filter { it.names.any { n -> n == "/$name" } }
            }.getOrElse {
                LOGGER.warn("Could not check for conflicting container '$name': ${it.message}")
                return
            }

            containers.forEach { container ->
                LOGGER.info("Removing conflicting container: $name")
                runCatching {
                    docker.removeContainerCmd(container.id).withForce(true).exec()
                }
            }
        }
    }

    override fun start(onProgress: (PullProgress) -> Unit): ComposeResult {
        // Returns Failure rather than hanging if Docker engine is off or paused
        val existing = runCatching {
            docker.listContainersCmd()
                .withLabelFilter(mapOf("com.docker.compose.project" to projectName))
                .withShowAll(true)
                .exec()
        }.getOrElse {
            LOGGER.error("Docker engine unavailable: ${it.message}")
            return ComposeResult.Failure(-1, "Docker engine is not running: ${it.message}")
        }

        if (existing.isNotEmpty()) {
            existing.filter { it.state != "running" }.forEach { container ->
                runCatching { docker.startContainerCmd(container.id).exec() }
            }
            return ComposeResult.Success("Started existing containers")
        }

        val file = writeComposeFile()
        return try {
            pullImages(onProgress)
            removeConflictingContainers(file)
            compose(file, "up", "-d")
        } finally {
            Files.deleteIfExists(file)
        }
    }

    // `stop` halts the containers but leaves them in place (-> ComposeStatus.Stopped); `down`
    // removes them entirely, which is what delete wants.
    override fun stop() = composeByProject("stop")
    override fun restart() = composeByProject("restart")
    override fun delete() = composeByProject("down", "--remove-orphans")

    override fun getSnapshot(): StackSnapshot {
        // A single container query backs both the run-state and the hash, so the two can never
        // disagree and we only pay for one round-trip per poll.
        // Returns Unknown/null rather than hanging if the Docker engine is off or paused.
        val containers = runCatching {
            docker.listContainersCmd()
                .withLabelFilter(mapOf("com.docker.compose.project" to projectName))
                .withShowAll(true)
                .exec()
        }.getOrElse {
            LOGGER.warn("Docker engine unavailable: ${it.message}")
            return StackSnapshot(ComposeStatus.Unknown, null)
        }

        val total = containers.size
        val status = if (total == 0) {
            ComposeStatus.NoStack
        } else {
            when (val running = containers.count { it.state == "running" }) {
                total -> ComposeStatus.Running
                0 -> ComposeStatus.Stopped
                else -> ComposeStatus.Partial(running, total)
            }
        }

        val remoteHash = containers.firstNotNullOfOrNull { it.labels["io.github.kindling.yaml-hash"] }
        return StackSnapshot(status, remoteHash)
    }

    override fun getStats(): Map<String, ContainerStats> {
        val containers = runCatching {
            docker.listContainersCmd()
                .withLabelFilter(mapOf("com.docker.compose.project" to projectName))
                .exec()
        }.getOrElse {
            LOGGER.warn("Docker engine unavailable: ${it.message}")
            return emptyMap()
        }.filter { it.state == "running" }

        val result = HashMap<String, ContainerStats>()
        for (container in containers) {
            val name = container.names.firstOrNull()?.removePrefix("/") ?: continue

            // One-shot read; blocking, but the caller runs this off the EDT.
            val stats = runCatching {
                docker.statsCmd(container.id)
                    .withNoStream(true)
                    .exec(InvocationBuilder.AsyncResultCallback<Statistics>())
                    .use { it.awaitResult() }
            }.getOrElse {
                LOGGER.warn("Failed to read stats for $name: ${it.message}")
                continue
            }

            val total = stats.cpuStats?.cpuUsage?.totalUsage ?: 0
            val system = stats.cpuStats?.systemCpuUsage ?: 0
            val cpus = (
                stats.cpuStats?.onlineCpus
                    ?: stats.cpuStats?.cpuUsage?.percpuUsage?.size?.toLong()
                    ?: 1L
                ).coerceAtLeast(1L)
            val cpuPercent = previousCpu[name]?.let { (prevTotal, prevSystem) ->
                val cpuDelta = total - prevTotal
                val systemDelta = system - prevSystem
                if (systemDelta > 0 && cpuDelta >= 0) cpuDelta.toDouble() / systemDelta * cpus * 100 else null
            }
            previousCpu[name] = total to system

            // Match `docker stats`: subtract reclaimable page cache from raw usage.
            val usage = stats.memoryStats?.usage ?: 0
            val cacheLike = stats.memoryStats?.stats?.let { it.inactiveFile ?: it.cache } ?: 0
            val used = (usage - cacheLike).coerceAtLeast(0)

            result[name] = ContainerStats(cpuPercent, used)
        }
        // Forget CPU history for containers that are no longer running.
        previousCpu.keys.retainAll(result.keys)
        return result
    }

    companion object {
        private val LOGGER = getLogger<ProcessComposeEngine>()

        val docker: DockerClient by lazy {
            val config = DefaultDockerClientConfig.createDefaultConfigBuilder().build()
            val httpClient = ZerodepDockerHttpClient.Builder()
                .dockerHost(config.dockerHost)
                .sslConfig(config.sslConfig)
                .build()
            DockerClientImpl.getInstance(config, httpClient)
        }
    }
}
