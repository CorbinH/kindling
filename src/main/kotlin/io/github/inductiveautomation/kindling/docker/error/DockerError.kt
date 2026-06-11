package io.github.inductiveautomation.kindling.docker.error

data class DockerError(
    val severity: Severity,
    val message: String,
    val relatedContainerNames: List<String> = emptyList(),
    val openConfig: () -> Unit,
) {
    enum class Severity { ERROR, WARNING }
}
