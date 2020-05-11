package org.veupathdb.service.demo.container.health;

import org.veupathdb.service.demo.generated.model.DependencyStatus.OnlineType;

/**
 * Service Dependency
 *
 * A wrapper for external resources providing methods needed for performing
 * health checks.
 */
public interface Dependency extends AutoCloseable {

  /**
   * Get the unique name of this dependency
   */
  String getName();

  /**
   * Test the resource availability
   */
  TestResult test();

  class TestResult {
    private final boolean reachable;
    private final OnlineType online;

    public TestResult(boolean reachable, OnlineType online) {
      this.reachable = reachable;
      this.online = online;
    }

    public boolean isReachable() {
      return reachable;
    }

    public OnlineType isOnline() {
      return online;
    }
  }
}
