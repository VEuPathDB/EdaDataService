package org.veupathdb.service.eda.compute.service

import org.veupathdb.lib.container.jaxrs.server.ContainerResources
import org.veupathdb.service.eda.compute.controller.ComputeController
import org.veupathdb.service.eda.compute.controller.ExpirationController

class Resources : ContainerResources(ServiceOptions) {
  override fun resources(): Array<Any> = arrayOf(
    ComputeController::class.java,
    JobsController::class.java,
    ExpirationController::class.java
  )
}
