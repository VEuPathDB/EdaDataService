package org.veupathdb.service.demo;

import org.veupathdb.service.demo.config.Options;
import org.veupathdb.service.demo.container.ContainerResources;
import org.veupathdb.service.demo.container.health.DependencyManager;
import org.veupathdb.service.demo.container.service.HealthService;
import org.veupathdb.service.demo.service.HelloWorld;

/**
 * Service Resource Registration.
 *
 * This is where all the individual service specific resources and middleware
 * should be registered.
 */
public class Resources extends ContainerResources {
  public Resources(Options opts) {
    super(opts,

      HelloWorld.class

    );
  }
}
