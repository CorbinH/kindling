package io.github.inductiveautomation.kindling.docker.engine

sealed class ComposeResult {
    data class Success(val output: String) : ComposeResult()
    data class Failure(val exitCode: Int, val output: String) : ComposeResult()
}
