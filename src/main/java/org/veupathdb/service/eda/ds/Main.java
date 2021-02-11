package org.veupathdb.service.eda.ds;

import org.veupathdb.lib.container.jaxrs.config.Options;
import org.veupathdb.lib.container.jaxrs.server.ContainerResources;
import org.veupathdb.lib.container.jaxrs.server.Server;

public class Main extends Server {

  public static void main(String[] args) {
    new Main().start(args);
  }

  @Override
  protected ContainerResources newResourceConfig(Options options) {
    return new Resources(options);
  }
}
