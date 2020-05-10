package org.veupathdb.service.demo;

import org.apache.logging.log4j.LogManager;
import org.veupathdb.service.demo.config.Options;
import org.veupathdb.service.demo.container.health.DependencyManager;
import org.veupathdb.service.demo.container.utils.Cli;
import org.veupathdb.service.demo.container.utils.DbManager;
import org.veupathdb.service.demo.container.utils.Log;

import javax.ws.rs.ApplicationPath;

import java.io.IOException;

import static java.lang.String.format;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory.createHttpServer;

@ApplicationPath("/")
public class Main {
  private static final int DEFAULT_PORT = 8080;

  public static void main(String[] args) throws IOException {
    // Configure Log4J and route all logging through it.
    Log.initialize();

    final var log    = LogManager.getLogger(Main.class);
    final var opts   = Cli.ParseCLI(args, Options.getInstance());
    final var port   = opts.getServerPort().orElse(DEFAULT_PORT);
    final var server = createHttpServer(fromUri("//0.0.0.0").port(port).build(),
      new Resources(opts));

    DbManager.initAccountDatabase(opts);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Server shutting down.");
      server.shutdownNow();
      DependencyManager.getInstance().shutDown();
    }));

    server.start();
    log.info(format("Server started.  Listening on port %d.", port));
  }
}
