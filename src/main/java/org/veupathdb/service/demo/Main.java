package org.veupathdb.service.demo;

import javax.ws.rs.ApplicationPath;

import java.io.IOException;

import org.veupathdb.lib.container.jaxrs.config.Options;
import org.veupathdb.lib.container.jaxrs.server.ContainerResources;
import org.veupathdb.lib.container.jaxrs.server.Server;

@ApplicationPath("/")
public class Main extends Server {
  public static void main(String[] args) throws IOException {
    var server = new Main();
    server.enableAccountDB();
    server.start(args);
  }

  @Override
  protected ContainerResources newResourceConfig(Options options) {
    return new Resources(options);
  }
}
