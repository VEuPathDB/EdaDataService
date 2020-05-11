package org.veupathdb.service.demo.container.health;

abstract public class ExternalDependency extends AbstractDependency {

  protected Pinger pinger;

  protected ExternalDependency(String name) {
    super(name);
    pinger = new Pinger();
  }

  public void setPinger(Pinger pinger) {
    this.pinger = pinger;
  }
}
