package io.github.inductiveautomation.kindling.docker.services

import io.github.inductiveautomation.kindling.docker.DockerDraftPanel
import io.github.inductiveautomation.kindling.docker.services.generic.GenericServiceTool
import io.github.inductiveautomation.kindling.docker.services.ignition.IgnitionServiceTool
import io.github.inductiveautomation.kindling.docker.services.model.DockerServiceModel
import io.github.inductiveautomation.kindling.docker.services.mssql.MssqlServiceTool
import javax.swing.Icon
import kotlin.collections.find
import kotlin.reflect.KClass

interface DockerServiceTool<M : DockerServiceModel, N : AbstractDockerServiceNode<M>> {
    val icon: Icon
    val name: String
    val defaultImage: String
    val modelClass: KClass<M>

    context(_: DockerDraftPanel)
    fun createModel(): M
    context(_: DockerDraftPanel)
    fun createNode(model: M): N

    fun DockerDraftPanel.init() = Unit

    companion object {
        val tools: List<DockerServiceTool<out DockerServiceModel, out AbstractDockerServiceNode<out DockerServiceModel>>> = listOf(
            IgnitionServiceTool,
            GenericServiceTool,
            MssqlServiceTool,
        )

        @Suppress("unchecked_cast")
        context(_: DockerDraftPanel)
        fun <T : DockerServiceModel> createNode(model: T): AbstractDockerServiceNode<T>? {
            val tool = tools.find {
                it.modelClass == model::class
            } as DockerServiceTool<T, AbstractDockerServiceNode<T>>?

            return tool?.createNode(model)
        }
    }
}
