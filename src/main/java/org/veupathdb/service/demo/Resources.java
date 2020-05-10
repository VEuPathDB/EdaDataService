package org.veupathdb.service.demo;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.veupathdb.service.demo.config.Options;
import org.veupathdb.service.demo.container.health.DependencyManager;
import org.veupathdb.service.demo.container.middleware.AuthFilter;
import org.veupathdb.service.demo.container.middleware.Log4JFilter;
import org.veupathdb.service.demo.container.middleware.RequestIdFilter;
import org.veupathdb.service.demo.container.service.HealthService;
import org.veupathdb.service.demo.container.utils.DbManager;
import org.veupathdb.service.demo.service.HelloWorld;

public class Resources extends ResourceConfig {
  public Resources(Options opts) {
    super(
      // Enable Jackson request/response (de)serialization.
      JacksonFeature.class,

      // Assigns each request a unique identifier used for metrics and logging.
      RequestIdFilter.class,

      // Request logging
      Log4JFilter.class,

      // Endpoint Implementations
      HelloWorld.class
    );

    // Register middleware types that require dependencies.
    register(new AuthFilter(opts, DbManager.getAccountDatabase()));
    register(new HealthService(DependencyManager.getInstance()));
  }
}
