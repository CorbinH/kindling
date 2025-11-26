package io.github.inductiveautomation.kindling.docker.services.generic

import io.github.inductiveautomation.kindling.docker.DockerDraftPanel
import io.github.inductiveautomation.kindling.docker.DockerTool
import io.github.inductiveautomation.kindling.docker.services.DockerServiceTool
import io.github.inductiveautomation.kindling.docker.services.model.DefaultDockerServiceModel
import io.github.inductiveautomation.kindling.docker.services.model.DockerServiceModel
import javax.swing.Icon

object GenericServiceTool : DockerServiceTool {
    override val icon: Icon = DockerTool.icon
    override val name: String = "Generic Docker Node"
    override val defaultImage: String = "ubuntu"

    context(panel: DockerDraftPanel)
    override fun createModel(): DefaultDockerServiceModel = DefaultDockerServiceModel(
        image = defaultImage,
        containerName = "Container-${panel.nodeIdManager.generateID()}",
    )

    context(panel: DockerDraftPanel)
    override fun createNode(model: DockerServiceModel): GenericDockerServiceNode {
        require(model is DefaultDockerServiceModel) {
            "Model ${model::class.java.name} is not a Default Service Model"
        }
        return GenericDockerServiceNode(model, panel.volumes, panel.networks.keys.toList())
    }

    override fun modelFromDefault(model: DefaultDockerServiceModel) = model

    override fun isValidCandidate(model: DefaultDockerServiceModel) = true
}
