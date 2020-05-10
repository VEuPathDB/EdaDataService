package org.veupathdb.service.demo.config;

import org.gusdb.fgputil.db.platform.SupportedPlatform;
import picocli.CommandLine;
import picocli.CommandLine.IHelpFactory;
import picocli.CommandLine.Option;
import picocli.CommandLine.UnmatchedArgumentException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

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

  @Option(
    names = {"-a", "--auth-secret"},
    defaultValue = "${env:AUTH_SECRET_KEY}",
    description = "env: AUTH_SECRET_KEY",
    arity = "1")
  private String authSecretKey;

  @Option(
    names = {"-o", "--server-port"},
    defaultValue = "${env:SERVER_PORT}",
    description = "env: SERVER_PORT",
    arity = "1")
  private Integer serverPort;

  @Option(
    names = {"-j", "--jdbc-url"},
    defaultValue = "${env:JDBC_URL}",
    description = "env: JDBC_URL",
    arity = "1")
  private String jdbcUrl;

  @Option(
    names = {"-u", "--db-user"},
    defaultValue = "${env:DB_USER}",
    description = "env: DB_USER",
    arity = "1")
  private String dbUser;

  @Option(
    names = {"-p", "--db-pass"},
    defaultValue = "${env:DB_PASS}",
    description = "env: DB_PASS",
    arity = "1")
  private String dbPass;

  @Option(
    names = {"-P", "--db-platform"},
    defaultValue = "${env:DB_PLATFORM}",
    description = "env: DB_PLATFORM",
    arity = "1")
  private SupportedPlatform dbPlatform;

  @Option(
    names = {"-s", "--db-pool-size"},
    defaultValue = "${env:DB_POOL_SIZE}",
    description = "env: DB_POOL_SIZE",
    arity = "1")
  private Integer dbPoolSize;

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

  public Optional<String> getJdbcUrl() {
    return Optional.ofNullable(jdbcUrl);
  }

  public Optional<String> getDbUser() {
    return Optional.ofNullable(dbUser);
  }

  public Optional<String> getDbPass() {
    return Optional.ofNullable(dbPass);
  }

  public Optional<SupportedPlatform> getDbPlatform() {
    return Optional.ofNullable(dbPlatform);
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
