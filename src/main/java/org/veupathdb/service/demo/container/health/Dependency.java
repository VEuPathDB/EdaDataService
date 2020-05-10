package org.veupathdb.service.demo.container.health;

import org.veupathdb.service.demo.generated.model.DependencyStatus.OnlineType;

public interface Dependency extends AutoCloseable {
  String getName();
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
