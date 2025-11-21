package io.github.inductiveautomation.kindling.docker.services.generic

import io.github.inductiveautomation.kindling.docker.DockerDraftPanel
import io.github.inductiveautomation.kindling.docker.services.DockerServiceTool
import io.github.inductiveautomation.kindling.docker.DockerTool
import io.github.inductiveautomation.kindling.docker.services.model.DefaultDockerServiceModel
import javax.swing.Icon
import kotlin.reflect.KClass

object GenericServiceTool : DockerServiceTool<DefaultDockerServiceModel, GenericDockerServiceNode> {
    override val icon: Icon = DockerTool.icon
    override val name: String = "Generic Docker Node"
    override val defaultImage: String = "kcollins/mssql:latest"
    override val modelClass: KClass<DefaultDockerServiceModel>
        get() = DefaultDockerServiceModel::class

    context(panel: DockerDraftPanel)
    override fun createModel(): DefaultDockerServiceModel {
        return DefaultDockerServiceModel(
            image = defaultImage,
            containerName = "Container-${panel.nodeIdManager.generateID()}",
        )
    }

    context(panel: DockerDraftPanel)
    override fun createNode(model: DefaultDockerServiceModel): GenericDockerServiceNode {
        return GenericDockerServiceNode(model, panel.volumes, panel.networks.keys.toList())
    }
}