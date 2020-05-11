package org.veupathdb.service.demo;

import org.veupathdb.service.demo.config.Options;
import org.veupathdb.service.demo.container.ResourceConfig;
import org.veupathdb.service.demo.container.health.DependencyManager;
import org.veupathdb.service.demo.container.service.HealthService;
import org.veupathdb.service.demo.service.HelloWorld;

public class Resources extends ResourceConfig {
  public Resources(Options opts) {
    super(opts,

      // Endpoint Implementations
      HelloWorld.class
    );

    // Register middleware types that require dependencies.
    register(new HealthService(DependencyManager.getInstance()));
  }
}
