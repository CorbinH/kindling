package io.github.inductiveautomation.kindling.docker.error

import io.github.inductiveautomation.kindling.docker.services.AbstractDockerServiceNode
import io.github.inductiveautomation.kindling.docker.services.ignition.model.IgnitionCommandLineArgument
import io.github.inductiveautomation.kindling.docker.services.ignition.model.IgnitionServiceModel
import io.github.inductiveautomation.kindling.docker.services.model.DockerServiceModel
import io.github.inductiveautomation.kindling.statistics.GatewayBackup
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

fun interface DockerErrorChecker {
    fun check(services: List<AbstractDockerServiceNode<*>>, baseDir: Path?): List<DockerError>

    companion object {
        val all: List<DockerErrorChecker> = listOf(
            DuplicateHostPortsChecker,
            DuplicateHostnamesChecker,
            DuplicateContainerNamesChecker,
            DuplicateCliFlagsChecker,
            DuplicateEnvVarsChecker,
            MissingGwbkFileChecker,
            GwbkVersionMismatchChecker,
        )
    }
}

private val RESTORE_FLAG = IgnitionCommandLineArgument.GWBK_RESTORE_PATH.flag

/**
 * Resolve a bind-mount host path against the compose-file's directory if relative,
 * or use it as-is when already absolute.
 */
private fun resolveBindPath(bindPath: String, baseDir: Path?): Path {
    val p = Path(bindPath)
    return if (p.isAbsolute || baseDir == null) p else baseDir.resolve(p)
}

/**
 * For each Ignition service that has a `-r <containerPath>` arg, find the matching
 * BindMount (where containerPath == arg value). Returns triples of (service, containerPath, bindPath).
 */
private fun gwbkRestoreBindings(services: List<DockerServiceModel>): List<Triple<DockerServiceModel, String, String>> =
    services.mapNotNull { model ->
        val restoreArg = model.commands.firstOrNull { it.startsWith("$RESTORE_FLAG ") } ?: return@mapNotNull null
        val containerPath = restoreArg.substringAfter("$RESTORE_FLAG ").trim()
        val mount = model.volumes.firstOrNull { it.containerPath == containerPath } ?: return@mapNotNull null
        Triple(model, containerPath, mount.bindPath)
    }

private object DuplicateHostPortsChecker : DockerErrorChecker {
    override fun check(services: List<AbstractDockerServiceNode<*>>, baseDir: Path?): List<DockerError> {
        data class Holder(val port: String, val containerName: String)

        val all = services.flatMap { svc ->
            svc.model.ports.map { Holder(it.published, svc.model.containerName) }
        }
        return all.groupBy { it.port }
            .filter { (_, holders) -> holders.size > 1 }
            .map { (port, holders) ->
                DockerError(
                    severity = DockerError.Severity.ERROR,
                    message = "Duplicate host port $port published by ${holders.size} services.",
                    relatedContainerNames = holders.map { it.containerName }.distinct(),
                    openConfig = {
                        val errorServices = holders.mapNotNull { (_, name) ->
                            services.find { it.model.containerName == name }
                        }

                        errorServices.forEach { s ->
                            s.openConfigWindow()
                        }
                    },
                )
            }
    }
}

private object DuplicateHostnamesChecker : DockerErrorChecker {
    override fun check(services: List<AbstractDockerServiceNode<*>>, baseDir: Path?): List<DockerError> =
        services
            .filterNot { it.model.hostName.isNullOrBlank() }
            .groupBy { it.model.hostName!! }
            .filter { (_, group) -> group.size > 1 }
            .map { (hostName, group) ->
                DockerError(
                    severity = DockerError.Severity.ERROR,
                    message = "Duplicate hostname \"$hostName\" assigned to ${group.size} services.",
                    relatedContainerNames = group.map { it.model.containerName },
                    openConfig = {
                        group.forEach {
                            it.openConfigWindow()
                        }
                    },
                )
            }
}

private object DuplicateContainerNamesChecker : DockerErrorChecker {
    override fun check(services: List<AbstractDockerServiceNode<*>>, baseDir: Path?): List<DockerError> =
        services
            .groupBy { it.model.containerName }
            .filter { (_, group) -> group.size > 1 }
            .map { (name, group) ->
                DockerError(
                    severity = DockerError.Severity.ERROR,
                    message = "Duplicate container name \"$name\" used by ${group.size} services.",
                    relatedContainerNames = listOf(name),
                    openConfig = {
                        group.forEach {
                            it.openConfigWindow()
                        }
                    },
                )
            }
}

private object DuplicateCliFlagsChecker : DockerErrorChecker {
    override fun check(services: List<AbstractDockerServiceNode<*>>, baseDir: Path?): List<DockerError> =
        services.flatMap { svc ->
            svc.model.commands
                .map { it.substringBefore(' ').trim() }
                .filter { it.startsWith("-") }
                .groupingBy { it }
                .eachCount()
                .filter { (_, count) -> count > 1 }
                .map { (flag, count) ->
                    DockerError(
                        severity = DockerError.Severity.ERROR,
                        message = "Service \"${svc.model.containerName}\" has CLI flag $flag $count times.",
                        relatedContainerNames = listOf(svc.model.containerName),
                        openConfig = {
                            svc.openConfigWindow()
                        },
                    )
                }
        }
}

private object DuplicateEnvVarsChecker : DockerErrorChecker {
    /**
     * The model stores environment as a Map, so duplicate keys can't exist at runtime.
     * Defensive check anyway — iterates the underlying entries in case storage ever changes
     * to a list-shape that does permit duplicates.
     */
    override fun check(services: List<AbstractDockerServiceNode<*>>, baseDir: Path?): List<DockerError> =
        services.flatMap { svc ->
            svc.model.environment.entries
                .groupingBy { it.key }
                .eachCount()
                .filter { (_, count) -> count > 1 }
                .map { (key, count) ->
                    DockerError(
                        severity = DockerError.Severity.ERROR,
                        message = "Service \"${svc.model.containerName}\" has environment variable $key defined $count times.",
                        relatedContainerNames = listOf(svc.model.containerName),
                        openConfig = {
                            svc.openConfigWindow()
                        },
                    )
                }
        }
}

private object MissingGwbkFileChecker : DockerErrorChecker {
    override fun check(services: List<AbstractDockerServiceNode<*>>, baseDir: Path?): List<DockerError> =
        gwbkRestoreBindings(services.map(AbstractDockerServiceNode<*>::model)).mapNotNull { (svc, _, bindPath) ->
            val resolved = resolveBindPath(bindPath, baseDir)
            if (resolved.exists()) {
                null
            } else {
                DockerError(
                    severity = DockerError.Severity.ERROR,
                    message = "Gateway backup file not found for \"${svc.containerName}\": $resolved",
                    relatedContainerNames = listOf(svc.containerName),
                    openConfig = {
                        val node = services.find { it.model === svc }
                        node?.openConfigWindow()
                    },
                )
            }
        }
}

private object GwbkVersionMismatchChecker : DockerErrorChecker {
    override fun check(services: List<AbstractDockerServiceNode<*>>, baseDir: Path?): List<DockerError> =
        gwbkRestoreBindings(services.map(AbstractDockerServiceNode<*>::model)).mapNotNull { (svc, _, bindPath) ->
            if (svc !is IgnitionServiceModel) return@mapNotNull null
            val resolved = resolveBindPath(bindPath, baseDir)
            if (!resolved.exists()) return@mapNotNull null  // covered by MissingGwbkFileChecker

            val gwbkVersion = runCatching {
                GatewayBackup(resolved).info
                    .getElementsByTagName("version")
                    .item(0)
                    ?.textContent
                    ?.split(".")
                    ?.take(3)
                    ?.joinToString(".")
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

            val imageVersion = svc.version
            // "latest" / "nightly" are intentional pins by the user — skip
            if (imageVersion == "latest" || imageVersion == "nightly" || imageVersion == gwbkVersion) {
                null
            } else {
                DockerError(
                    severity = DockerError.Severity.WARNING,
                    message = "Image version $imageVersion does not match gateway backup version $gwbkVersion for \"${svc.containerName}\".",
                    relatedContainerNames = listOf(svc.containerName),
                    openConfig = {
                        val node = services.find { it.model === svc }
                        node?.openConfigWindow()
                    },
                )
            }
        }
}
