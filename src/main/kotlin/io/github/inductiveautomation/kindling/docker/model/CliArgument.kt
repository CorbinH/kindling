package io.github.inductiveautomation.kindling.docker.model

typealias CliArgument = String

internal val CLI_REGEX = """(?:-\S* )?\S*""".toRegex()

fun CliArgument.isValid(): Boolean {
    return CLI_REGEX.matches(this)
}
