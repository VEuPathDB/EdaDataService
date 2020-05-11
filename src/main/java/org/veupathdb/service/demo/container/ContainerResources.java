package org.veupathdb.service.demo.container;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.veupathdb.service.demo.config.Options;
import org.veupathdb.service.demo.container.health.DependencyManager;
import org.veupathdb.service.demo.container.middleware.AuthFilter;
import org.veupathdb.service.demo.container.middleware.PrometheusFilter;
import org.veupathdb.service.demo.container.middleware.RequestIdFilter;
import org.veupathdb.service.demo.container.middleware.RequestLogger;
import org.veupathdb.service.demo.container.service.ApiDocService;
import org.veupathdb.service.demo.container.service.HealthService;
import org.veupathdb.service.demo.container.service.MetricsService;
import org.veupathdb.service.demo.container.utils.DbManager;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Container Meta Resources
 *
 * Universal services that should be available in all containerized services.
 *
 * This class is intended for framework internal use and is subject to change
 * with framework updates.
 */
public class ContainerResources extends org.glassfish.jersey.server.ResourceConfig {
  private static final Class<?>[] DEFAULT_CLASSES = {
    JacksonFeature.class,
    PrometheusFilter.class,
    RequestIdFilter.class,
    RequestLogger.class,
  };

  public ContainerResources(Options opts, Class<?>... classes) {
    var providers = Arrays.stream(classes).collect(Collectors.toSet());
    providers.addAll(Arrays.asList(DEFAULT_CLASSES));

    registerClasses(providers)
      .register(new AuthFilter(opts, DbManager.getAccountDatabase()));

    registerInstances(
      new ApiDocService(),
      new HealthService(DependencyManager.getInstance()),
      new MetricsService()
    );
  }
}
