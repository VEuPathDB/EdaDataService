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
      throw new IllegalStateException("Options.getInstance() was called before "
        + "the Options object was initialized.");
    return instance;
  }

  public static Options initialize(String[] args) {
    var tmp = new Options();
    var cli = new CommandLine(tmp)
      .setCaseInsensitiveEnumValuesAllowed(true)
      .setUnmatchedArgumentsAllowed(false);
    try {
      cli.parseArgs(args);
    } catch (UnmatchedArgumentException e) {
      var match = e.getUnmatched()
        .stream()
        .anyMatch(((Predicate<String>)"-h"::equals).or("--help"::equals));

      if (match) {
        cli.usage(System.out);
        Runtime.getRuntime().exit(0);
      }

      throw new RuntimeException("Unrecognized argument(s) " + e.getUnmatched());
    }

    emptyToNull(tmp);
    return instance = tmp;
  }

  static void emptyToNull(Options opts) {
    try {
      for (var prop : opts.getClass().getDeclaredFields()) {
        var tmp = prop.get(opts);
        if (Objects.isNull(tmp))
          continue;

        if (tmp.equals("")) {
          prop.set(opts, null);
        } else if (tmp.equals(0)) {
          prop.set(opts, null);
        }
      }
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
