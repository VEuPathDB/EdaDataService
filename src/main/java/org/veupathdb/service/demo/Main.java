package org.veupathdb.service.demo;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.veupathdb.service.demo.service.HelloWorld;

import javax.ws.rs.ApplicationPath;
import java.io.IOException;
import java.net.URI;

import static org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory.createHttpServer;

@ApplicationPath("/")
public class Main extends ResourceConfig {

  public Main() {
    super(
      // Features
      JacksonFeature.class,

      // Endpoint Implementations
      HelloWorld.class
    );
  }

  public static void main(String[] args) throws IOException {
    createHttpServer(URI.create("//0.0.0.0:8080"), new Main())
      .start();
  }
}
