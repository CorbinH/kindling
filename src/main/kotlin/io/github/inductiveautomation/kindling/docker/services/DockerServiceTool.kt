package io.github.inductiveautomation.kindling.docker.services

import io.github.inductiveautomation.kindling.docker.DockerDraftPanel
import io.github.inductiveautomation.kindling.docker.services.generic.GenericServiceTool
import io.github.inductiveautomation.kindling.docker.services.ignition.IgnitionServiceTool
import io.github.inductiveautomation.kindling.docker.services.model.DefaultDockerServiceModel
import io.github.inductiveautomation.kindling.docker.services.model.DockerServiceModel
import io.github.inductiveautomation.kindling.docker.services.mssql.MssqlServiceTool
import javax.swing.Icon

interface DockerServiceTool {
    val icon: Icon
    val name: String
    val defaultImage: String

    context(_: DockerDraftPanel)
    fun createModel(): DockerServiceModel
    context(_: DockerDraftPanel)
    fun createNode(model: DockerServiceModel): AbstractDockerServiceNode<*>

    fun DockerDraftPanel.init() = Unit

    fun isValidCandidate(model: DefaultDockerServiceModel): Boolean

    fun modelFromDefault(model: DefaultDockerServiceModel): DockerServiceModel

    companion object {
        val tools = listOf(
            IgnitionServiceTool,
            MssqlServiceTool,
            GenericServiceTool, // Must be last in list
        )
    }
}
