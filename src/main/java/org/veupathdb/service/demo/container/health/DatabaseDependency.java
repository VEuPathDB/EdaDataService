package org.veupathdb.service.demo.container.health;

import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.veupathdb.service.demo.generated.model.DependencyStatus.OnlineType;

import java.sql.SQLException;

import static org.apache.logging.log4j.LogManager.getLogger;

/**
 * Database Dependency
 *
 * Dependency wrapper for a database instance.
 */
public class DatabaseDependency implements Dependency {
  private static final Logger LOG = getLogger(DatabaseDependency.class);

  private final String name;
  private final String url;
  private final int port;
  private final DatabaseInstance ds;

  private String testQuery = "SELECT 1 FROM dual";

  public DatabaseDependency(String name, String url, int port, DatabaseInstance ds) {
    this.name = name;
    this.ds = ds;
    this.url = url;
    this.port = port;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public TestResult test() {
    LOG.info("Checking dependency health for database {}", name);

    if (!Pinger.isReachable(url, port))
      return new TestResult(false, OnlineType.UNKNOWN);

    try (
      var con  = ds.getDataSource().getConnection();
      var stmt = con.createStatement();
    ) {
      stmt.execute(testQuery);
      return new TestResult(true, OnlineType.YES);
    } catch (SQLException e) {
      LOG.warn("Health check failed for database {}", name);
      LOG.debug(e);
      return new TestResult(true, OnlineType.UNKNOWN);
    }
  }

  @Override
  public void close() throws Exception {
    ds.close();
  }

  public void setTestQuery(String testQuery) {
    this.testQuery = testQuery;
  }
}
