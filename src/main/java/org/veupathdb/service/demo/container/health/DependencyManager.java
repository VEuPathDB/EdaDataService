package org.veupathdb.service.demo.container.health;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.veupathdb.service.demo.container.health.Dependency.TestResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.synchronizedMap;
import static java.util.Objects.isNull;

/**
 * Manager class for service dependencies.
 */
public final class DependencyManager {
  private DependencyManager() {}

  private static DependencyManager instance;

  private final Logger log = LogManager.getLogger(DependencyManager.class);
  private final Map<String, Dependency> dependencies = synchronizedMap(new HashMap<>());

  /**
   * Registers a new service dependency.
   *
   * @throws RuntimeException if the dependency given already exists or has a
   * name that conflicts with an existing dependency.
   */
  public void register(Dependency dependency) {
    if (dependencies.containsKey(dependency.getName()))
      throw new RuntimeException("More than one dependency defined with the name " + dependency.getName());
    dependencies.put(dependency.getName(), dependency);
  }

  /**
   * Looks up a dependency by name.
   *
   * @return an Option of Dependency that will be some if a dependency exists
   * with the given name and will be none if no such dependency was found.
   */
  public Optional<Dependency> lookupDependency(String name) {
    return Optional.ofNullable(dependencies.get(name));
  }

  /**
   * Removes a dependency by name.
   *
   * If the dependency does not already exist, does nothing.
   */
  public void removeDependency(String name) {
    dependencies.remove(name);
  }

  /**
   * Removes a dependency.
   *
   * If the dependency does not already exist, does nothing.
   */
  public void removeDependency(Dependency dep) {
    dependencies.remove(dep.getName());
  }

  /**
   * Runs the test method on all currently registered dependencies and returns
   * a map of the test results keyed on dependency name.
   */
  public Map<String, Dependency.TestResult> testDependencies() {
    return dependencies.entrySet()
      .parallelStream()
      .map(e -> new DependencyPair(e.getKey(), e.getValue().test()))
      .collect(Collectors.toUnmodifiableMap(k -> k.name, v -> v.result));
  }

  /**
   * Attempts to shut down all dependencies currently registered.
   */
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

class DependencyPair {
  final String name;
  final TestResult result;

  DependencyPair(String name, TestResult result) {
    this.name = name;
    this.result = result;
  }
}
