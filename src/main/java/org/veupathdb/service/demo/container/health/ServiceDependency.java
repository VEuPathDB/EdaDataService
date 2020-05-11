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
abstract public class ServiceDependency extends ExternalDependency {

  private final Logger log = LogManager.getLogger(getClass());

  private final String url;
  private final int port;

  public ServiceDependency(String name, String url, int port) {
    super(name);
    this.url = url;
    this.port = port;
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

    if (!pinger.isReachable(url, port))
      return new TestResult(false, OnlineType.UNKNOWN);

    return serviceTest();
  }

  abstract TestResult serviceTest();

  @Override
  public void close() {}
}
