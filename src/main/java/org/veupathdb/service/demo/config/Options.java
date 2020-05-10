package org.veupathdb.service.demo.config;

import org.gusdb.fgputil.db.platform.SupportedPlatform;
import picocli.CommandLine.Option;

import java.util.Objects;
import java.util.Optional;

/**
 * CLI Options.
 */
public final class Options {
  private Options() {}

  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃                                                                      ┃
    ┃    Configuration Properties                                          ┃
    ┃                                                                      ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  @Option(names = "--auth-secret", defaultValue = "${env:AUTH_SECRET_KEY}", description = "env: AUTH_SECRET_KEY", arity = "1")
  private String authSecretKey;

  @Option(names = "--server-port", defaultValue = "${env:SERVER_PORT}", description = "env: SERVER_PORT", arity = "1")
  private Integer serverPort;

  @Option(names = "--db-host", defaultValue = "${env:DB_HOST}", description = "env: DB_HOST", arity = "1")
  private String dbHost;

  @Option(names = "--db-port", defaultValue = "${env:DB_PORT}", description = "env: DB_PORT", arity = "1")
  private Integer dbPort;

  @Option(names = "--db-user", defaultValue = "${env:DB_USER}", description = "env: DB_USER", arity = "1")
  private String dbUser;

  @Option(names = "--db-name", defaultValue = "${env:DB_NAME}", description = "env: DB_NAME", arity = "1")
  private String dbName;

  @Option(names = "--db-pass", defaultValue = "${env:DB_PASS}", description = "env: DB_PASS", arity = "1")
  private String dbPass;

  @Option(names = "--db-pool-size", defaultValue = "${env:DB_POOL_SIZE}", description = "env: DB_POOL_SIZE", arity = "1")
  private Integer dbPoolSize;

  @Option(names = "--db-platform", defaultValue = "${env:DB_PLATFORM}", description = "env: DB_PLATFORM", arity = "1")
  private SupportedPlatform dbPlatform;


  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃                                                                      ┃
    ┃    Property Getters                                                  ┃
    ┃                                                                      ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  public Optional<String> getAuthSecretKey() {
    return Optional.ofNullable(authSecretKey);
  }

  public Optional<Integer> getServerPort() {
    return Optional.ofNullable(serverPort);
  }

  public Optional<String> getDbUser() {
    return Optional.ofNullable(dbUser);
  }

  public Optional<String> getDbPass() {
    return Optional.ofNullable(dbPass);
  }

  public Optional<String> getDbHost() {
    return Optional.ofNullable(dbHost);
  }

  public Optional<String> getDbName() {
    return Optional.ofNullable(dbName);
  }

  public Optional<SupportedPlatform> getDbPlatform() {
    return Optional.ofNullable(dbPlatform);
  }

  public Optional<Integer> getDbPort() {
    return Optional.ofNullable(dbPort);
  }

  public Optional<Integer> getDbPoolSize() {
    return Optional.ofNullable(dbPoolSize);
  }

  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃                                                                      ┃
    ┃    Static Properties & Methods                                       ┃
    ┃                                                                      ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  private static Options instance;

  public static Options getInstance() {
    if (Objects.isNull(instance))
      instance = new Options();
    return instance;
  }
}
