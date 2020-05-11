package org.veupathdb.service.demo.container.health;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.veupathdb.service.demo.generated.model.DependencyStatus.OnlineType;

/**
 * Service Dependency
 *
 * A dependency wrapper for an external HTTP service.
 *
 * Implementations of this wrapper provide the specifics of how to check the
 * health status of the external service.
 */
abstract public class ServiceDependency implements Dependency {

  private final Logger log = LogManager.getLogger(getClass());

  private final String name;
  private final String url;
  private final int port;

  public ServiceDependency(String name, String url, int port) {
    this.name = name;
    this.url = url;
    this.port = port;
  }

  @Override
  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }

  public int getPort() {
    return port;
  }

  @Override
  public TestResult test() {
    log.info("Checking dependency health for external service {}", name);

    if (!Pinger.isReachable(url, port))
      return new TestResult(false, OnlineType.UNKNOWN);

    return serviceTest();
  }

  abstract TestResult serviceTest();

  @Override
  public void close() throws Exception {}
}
