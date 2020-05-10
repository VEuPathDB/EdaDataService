package org.veupathdb.service.demo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.veupathdb.service.demo.config.Options;
import org.veupathdb.service.demo.middleware.AuthFilter;
import org.veupathdb.service.demo.middleware.Log4JFilter;
import org.veupathdb.service.demo.middleware.RequestIdFilter;
import org.veupathdb.service.demo.service.HelloWorld;
import org.veupathdb.service.demo.utils.DbManager;
import org.veupathdb.service.demo.utils.Log;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.UriBuilder;

import java.io.IOException;

import static org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory.createHttpServer;
import static org.veupathdb.service.demo.utils.Errors.silence;

@ApplicationPath("/")
public class Main extends ResourceConfig {
  private static final int DEFAULT_PORT = 8080;

  private static Logger log;

  public Main(Options opts, DatabaseInstance acctDb) {

    super(
      // Features
      JacksonFeature.class,

      // Middleware
      RequestIdFilter.class,
      Log4JFilter.class,

      // Endpoint Implementations
      HelloWorld.class
    );
    register(new AuthFilter(opts, acctDb));
  }

  public static void main(String[] args) throws IOException {
    // Configure Log4J and route all logging through it.
    Log.initialize();
    log = LogManager.getLogger(Main.class);

    final var opts = Options.initialize(args);
    validateOptions(opts);

    final var acctDb = DbManager.initAccountDatabase(opts);

    final var server = createHttpServer(
      UriBuilder.fromUri("//0.0.0.0")
        .port(opts.getServerPort().orElse(DEFAULT_PORT))
        .build(),
      new Main(opts, acctDb));

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      server.shutdownNow();
      silence(acctDb::close);
    }));
  }

  /**
   * Fail fast: we know we need the AUTH_SECRET environment variable as well
   * as the database settings for this service.  If your service does not
   * require authentication, remove this check to remove the config
   * requirements.
   */
  static void validateOptions(Options opts) {
    var ok = true;
    if (opts.getAuthSecretKey().isEmpty()) {
      ok = false;
      log.error("Missing required auth secret key parameter.");
    }
    if (opts.getJdbcUrl().isEmpty()) {
      ok = false;
      log.error("Missing required database connection URL parameter.");
    }
    if (opts.getDbUser().isEmpty()) {
      ok = false;
      log.error("Missing required database username parameter.");
    }
    if (opts.getDbPass().isEmpty()) {
      ok = false;
      log.error("Missing required database password parameter.");
    }

    if (!ok) {
      log.error("Use --help to view required parameters.");
      System.exit(1);
    }
  }
}
