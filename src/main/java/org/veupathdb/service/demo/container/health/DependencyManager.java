package org.veupathdb.service.demo.container.health;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.synchronizedMap;
import static java.util.Objects.isNull;

public final class DependencyManager {
  private DependencyManager() {}

  private static record Pair(String name, Dependency.TestResult result) {}

  private static DependencyManager instance;

  private final Logger log = LogManager.getLogger(DependencyManager.class);
  private final Map<String, Dependency> dependencies = synchronizedMap(new HashMap<>());

  public void register(Dependency dependency) {
    if (dependencies.containsKey(dependency.getName()))
      throw new RuntimeException("More than one dependency defined with the name " + dependency.getName());
    dependencies.put(dependency.getName(), dependency);
  }

  public Map<String, Dependency.TestResult> testDependencies() {
    return dependencies.entrySet()
      .parallelStream()
      .map(e -> new Pair(e.getKey(), e.getValue().test()))
      .collect(Collectors.toUnmodifiableMap(k -> k.name, v -> v.result));
  }

  public void shutDown() {
    for(var dep : dependencies.values()) {
      try {
        dep.close();
      } catch (Exception e) {
        log.error("Failed to shut down dependency " + dep.getName(), e);
      }
    }
  }

  public static DependencyManager getInstance() {
    if (isNull(instance))
      instance = new DependencyManager();
    return instance;
  }
}
