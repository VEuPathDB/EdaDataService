package org.veupathdb.service.demo.container.health;

abstract public class AbstractDependency implements Dependency {

  protected final String name;

  protected AbstractDependency(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }
}
