package io.github.inductiveautomation.kindling.docker.engine

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient
import io.github.inductiveautomation.kindling.utils.getLogger
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path

class ProcessComposeEngine(
    private var projectName: String,
    private val composeFileWriter: (OutputStream) -> Unit,
) : DockerComposeEngine {

    private fun writeComposeFile(): Path {
        val tempFile = Files.createTempFile("kindling-compose-", ".yaml")
        tempFile.toFile().outputStream().use(composeFileWriter)
        return tempFile
    }

    private fun compose(file: Path, vararg args: String): ComposeResult {
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
            val containers = docker.listContainersCmd()
                .withNameFilter(listOf(name))
                .withShowAll(true)
                .exec()
                .filter { it.names.any { n -> n == "/$name" } }

            containers.forEach { container ->
                LOGGER.info("Removing conflicting container: $name")
                runCatching {
                    docker.removeContainerCmd(container.id).withForce(true).exec()
                }
            }
        }
    }

    override fun start(): ComposeResult {
        val existing = docker.listContainersCmd()
            .withLabelFilter(mapOf("com.docker.compose.project" to projectName))
            .withShowAll(true)
            .exec()

        if (existing.isNotEmpty()) {
            existing.filter { it.state != "running" }.forEach { container ->
                runCatching { docker.startContainerCmd(container.id).exec() }
            }
            return ComposeResult.Success("Started existing containers")
        }

        val file = writeComposeFile()
        return try {
            removeConflictingContainers(file)
            compose(file, "up", "-d")
        } finally {
            Files.deleteIfExists(file)
        }
    }

    override fun stop() = composeByProject("down")
    override fun restart() = composeByProject("restart")
    override fun delete() = composeByProject("down", "--remove-orphans")

    override fun getStatus(): ComposeStatus {
        val containers = docker.listContainersCmd()
            .withLabelFilter(mapOf("com.docker.compose.project" to projectName))
            .withShowAll(true)
            .exec()

        val total = containers.size
        if (total == 0) return ComposeStatus.Stopped

        val running = containers.count { it.state == "running" }
        return when (running) {
            total -> ComposeStatus.Running
            0 -> ComposeStatus.Stopped
            else -> ComposeStatus.Partial(running, total)
        }
    }

    override fun getRemoteHash(): String? {
        val containers = docker.listContainersCmd()
            .withLabelFilter(mapOf("com.docker.compose.project" to projectName))
            .withShowAll(true)
            .exec()

        return containers.firstNotNullOfOrNull { it.labels["io.github.kindling.yaml-hash"] }
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
