package org.veupathdb.service.demo.container.utils;

import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.pool.SimpleDbConfig;
import org.veupathdb.service.demo.config.Options;

import java.util.Objects;

/**
 * Database Manager.
 *
 * Handles the connection to DatabaseInstances and provides singleton access to
 * them if needed.
 */
public final class DbManager implements AutoCloseable {

  private static final SupportedPlatform DEFAULT_PLATFORM = SupportedPlatform.ORACLE;
  private static final int DEFAULT_POOL_SIZE = 20;

  private DbManager() {}

  private static DatabaseInstance acctDb;

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
    return acctDb = new DatabaseInstance(SimpleDbConfig.create(
      opts.getDbPlatform().orElse(DEFAULT_PLATFORM),
      opts.getJdbcUrl().orElseThrow(),
      opts.getDbUser().orElseThrow(),
      opts.getDbPass().orElseThrow(),
      opts.getDbPoolSize().orElse(DEFAULT_POOL_SIZE)
    ));
  }

  public static DatabaseInstance getAccountDatabase() {
    if (Objects.isNull(acctDb))
      throw new IllegalStateException("Database.getAccountDatabase() was called"
        + " before the account database connection was initialized.");
    return acctDb;
  }

  @Override
  public void close() throws Exception {
    acctDb.close();
  }
}
