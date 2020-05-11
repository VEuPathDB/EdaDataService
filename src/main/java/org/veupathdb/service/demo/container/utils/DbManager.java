package org.veupathdb.service.demo.container.utils;

import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.pool.SimpleDbConfig;
import org.veupathdb.service.demo.config.InvalidConfigException;
import org.veupathdb.service.demo.config.Options;
import org.veupathdb.service.demo.container.health.DatabaseDependency;
import org.veupathdb.service.demo.container.health.DependencyManager;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Database Manager.
 *
 * Handles the connection to DatabaseInstances and provides singleton access to
 * them if needed.
 */
public final class DbManager {
  private DbManager() {}

  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃                                                                      ┃
    ┃    Error Messages                                                    ┃
    ┃                                                                      ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  private static final String
    ERR_NO_ACCTDB_HOST = "No configured account database host",
    ERR_NO_ACCTDB_NAME = "No configured account database name",
    ERR_NO_ACCTDB_USER = "No configured account database username",
    ERR_NO_ACCTDB_PASS = "No configured account database password",
    ERR_ACCTDB_NOT_INIT = "Database.getAccountDatabase() was called before the "
      + "account database connection was initialized.";


  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃                                                                      ┃
    ┃    Oracle Related Constants                                          ┃
    ┃                                                                      ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/


  /**
   * Default Oracle DB port
   */
  private static final int ORACLE_PORT = 1521;

  /**
   * Oracle connection string template
   */
  private static final String ORACLE_URL = "jdbc:oracle:oci:@//%s:%d/%s";


  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃                                                                      ┃
    ┃    Postgres Related Constants                                        ┃
    ┃                                                                      ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/


  /**
   * Default Postgres DB port
   */
  private static final int POSTGRES_PORT = 5432;

  /**
   * Postgres connection string template.
   */
  private static final String POSTGRES_URL = "jdbc:postgresql://%s:%d/%s";


  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃                                                                      ┃
    ┃    General Constants                                                 ┃
    ┃                                                                      ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  private static final SupportedPlatform DEFAULT_PLATFORM =
    SupportedPlatform.ORACLE;
  private static final int DEFAULT_POOL_SIZE = 20;


  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃                                                                      ┃
    ┃    Database Instances                                                ┃
    ┃                                                                      ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  private static DatabaseInstance acctDb;


  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃                                                                      ┃
    ┃    Account Database                                                  ┃
    ┃                                                                      ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  /**
   * Initialize a connection wrapper to the user account database.
   *
   * @param opts Configuration options
   *
   * @return the initialized DatabaseInstance
   */
  public static DatabaseInstance initAccountDatabase(Options opts) {
    if (Objects.nonNull(acctDb))
      return acctDb;

    var platform = opts.getDbPlatform().orElse(DEFAULT_PLATFORM);

    acctDb = new DatabaseInstance(SimpleDbConfig.create(
      platform,
      makeJdbcUrl(platform, opts),
      opts.getDbUser().orElseThrow(confErr(ERR_NO_ACCTDB_USER)),
      opts.getDbPass().orElseThrow(confErr(ERR_NO_ACCTDB_PASS)),
      opts.getDbPoolSize().orElse(DEFAULT_POOL_SIZE)
    ));

    //noinspection OptionalGetWithoutIsPresent
    DependencyManager.getInstance().register(new DatabaseDependency(
      "account-db", opts.getDbHost().get(), opts.getDbPort().orElse(
        platform == DEFAULT_PLATFORM ? ORACLE_PORT : POSTGRES_PORT), acctDb));

    return acctDb;
  }

  public static DatabaseInstance getAccountDatabase() {
    if (Objects.isNull(acctDb))
      throw new IllegalStateException(ERR_ACCTDB_NOT_INIT);
    return acctDb;
  }


  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃                                                                      ┃
    ┃    Public Helpers                                                    ┃
    ┃                                                                      ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  /**
   * Creates an Oracle JDBC connection string from the given options.
   */
  public static String makeOracleJdbcUrl(Options opts) {
    return String.format(ORACLE_URL,
      opts.getDbHost().orElseThrow(confErr(ERR_NO_ACCTDB_HOST)),
      opts.getDbPort().orElse(ORACLE_PORT),
      opts.getDbName().orElseThrow(confErr(ERR_NO_ACCTDB_NAME)));
  }

  /**
   * Creates a Postgres JDBC connection string from the given options.
   */
  public static String makePostgresJdbcUrl(Options opts) {
    return String.format(POSTGRES_URL,
      opts.getDbHost().orElseThrow(confErr(ERR_NO_ACCTDB_HOST)),
      opts.getDbPort().orElse(POSTGRES_PORT),
      opts.getDbName().orElseThrow(confErr(ERR_NO_ACCTDB_NAME)));
  }


  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃                                                                      ┃
    ┃    Internal Helpers                                                  ┃
    ┃                                                                      ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  private static String makeJdbcUrl(SupportedPlatform platform, Options opts) {
    return switch (platform) {
      case ORACLE -> makeOracleJdbcUrl(opts);
      case POSTGRESQL -> makePostgresJdbcUrl(opts);
    };
  }

  private static Supplier<InvalidConfigException> confErr(String message) {
    return () -> new InvalidConfigException(message);
  }
}
