package io.github.inductiveautomation.kindling.docker.compose

import io.github.inductiveautomation.kindling.docker.compose.model.Build

@Suppress("unused")
class BuildEditor(data: Build) : ComposeObjectEditor<Build>("Build", data) {
    val context by text(value = data.context) {
        data.context = it ?: "."
    }
    val dockerFile by text(value = data.dockerfile) {
        data.dockerfile = it
    }
    val dockerFileInline by text(value = data.dockerfileInline) {
        data.dockerfileInline = it
    }
    val args by map(value = data.args)
    val ssh by map(value = data.ssh)
    val cacheFrom by list("Cache From", data.cacheFrom)
    val cacheTo by list("Cache To", data.cacheTo)
    val extraHosts by list("Extra Hosts", data.extraHosts)
    val isolation by text(value = data.isolation) {
        data.isolation = it
    }
    val privileged by checkbox(value = data.privileged) {
        data.privileged = it
    }
    val labels by map(value = data.labels)
    val noCache by checkbox(value = data.noCache) {
        data.noCache = it
    }
    val pull by checkbox(value = data.pull) {
        data.pull = it
    }
    val secrets by list(value = data.secrets)
    val target by text(value = data.target) {
        data.target = it
    }
    val shmSize by text(value = data.shmSize) {
        data.shmSize = it
    }
    val uLimits by map(value = data.ulimits)
    val platforms by list(value = data.platforms)
}