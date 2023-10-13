package org.veupathdb.service.eda.user.stubdb;

import org.gusdb.fgputil.db.SqlScriptRunner;
import org.hsqldb.jdbc.JDBCDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

public class StubDb {

  private static final String DB_SCHEMA_SCRIPT = "org/veupathdb/service/eda/user/stubdb/createDbSchema.sql";

  private static final String STUB_DB_NAME = "stubDb";

  private static DataSource _ds;

  public static DataSource getDataSource() {
    if (_ds == null) {
      synchronized(StubDb.class) {
        if (_ds == null) _ds = loadDataSource();
      }
    }
    return _ds;
  }

  private static DataSource loadDataSource() {
    try {
      JDBCDataSource ds = new JDBCDataSource();
      ds.setDatabase("jdbc:hsqldb:mem:" + STUB_DB_NAME);
      ds.setUser("stubby");
      ds.setPassword("");
      SqlScriptRunner.runSqlScript(ds, DB_SCHEMA_SCRIPT);
      return ds;
    }
    catch (SQLException | IOException e) {
      throw new RuntimeException("Unable to load stub database", e);
    }
  }
}
