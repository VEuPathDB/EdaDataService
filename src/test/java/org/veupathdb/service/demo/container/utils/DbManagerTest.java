package org.veupathdb.service.demo.container.utils;

import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.veupathdb.service.demo.config.InvalidConfigException;
import org.veupathdb.service.demo.config.Options;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DbManagerTest {

  @BeforeEach
  void setUp() throws Exception {
    var optIn = Options.class.getDeclaredField("instance");
    optIn.setAccessible(true);
    optIn.set(null, null);

    var dbManIn = DbManager.class.getDeclaredField("instance");
    dbManIn.setAccessible(true);
    dbManIn.set(null, null);
  }

  @Test
  void getAccountDatabaseExists() throws Exception {
    var mk = mock(DatabaseInstance.class);
    var fd = DbManager.class.getDeclaredField("acctDb");
    fd.setAccessible(true);
    fd.set(null, mk);

    assertSame(mk, DbManager.accountDatabase());
  }

  @Test
  void getAccountDatabaseNotExists() throws Exception {
    var fd = DbManager.class.getDeclaredField("acctDb");
    fd.setAccessible(true);
    fd.set(null, null);

    assertThrows(IllegalStateException.class, DbManager::accountDatabase);
  }

  @Test
  void makeOracleJdbcUrl() throws Exception {
    var opts  = Options.getInstance();
    var dbMan = DbManager.getInstance();

    // No host, no name
    assertThrows(InvalidConfigException.class,
      () -> dbMan.makeOracleJdbcUrl(opts));

    var host = Options.class.getDeclaredField("dbHost");
    host.setAccessible(true);
    host.set(opts, "");

    // No name
    assertThrows(InvalidConfigException.class,
      () -> dbMan.makeOracleJdbcUrl(opts));

    var name = Options.class.getDeclaredField("dbName");
    name.setAccessible(true);
    name.set(opts, "");

    assertNotNull(dbMan.makeOracleJdbcUrl(opts));
  }

  @Test
  void makePostgresJdbcUrl() throws Exception {
    var opts  = Options.getInstance();
    var dbMan = DbManager.getInstance();

    // No host, no name
    assertThrows(InvalidConfigException.class,
      () -> dbMan.makePostgresJdbcUrl(opts));

    var host = Options.class.getDeclaredField("dbHost");
    host.setAccessible(true);
    host.set(opts, "");

    // No name
    assertThrows(InvalidConfigException.class,
      () -> dbMan.makePostgresJdbcUrl(opts));

    var name = Options.class.getDeclaredField("dbName");
    name.setAccessible(true);
    name.set(opts, "");

    assertNotNull(dbMan.makePostgresJdbcUrl(opts));
  }

  @Test
  void initDbConfig() throws Exception {
    var opts  = Options.getInstance();
    var dbMan = DbManager.getInstance();
    // No host
    assertThrows(InvalidConfigException.class,
      () -> dbMan.initDbConfig(opts));

    var host = Options.class.getDeclaredField("dbHost");
    host.setAccessible(true);
    host.set(opts, "");

    // No name
    assertThrows(InvalidConfigException.class,
      () -> dbMan.initDbConfig(opts));

    var name = Options.class.getDeclaredField("dbName");
    name.setAccessible(true);
    name.set(opts, "");

    // No user
    assertThrows(InvalidConfigException.class,
      () -> dbMan.initDbConfig(opts));

    var user = Options.class.getDeclaredField("dbUser");
    user.setAccessible(true);
    user.set(opts, "");

    // No password
    assertThrows(InvalidConfigException.class,
      () -> dbMan.initDbConfig(opts));

    var pass = Options.class.getDeclaredField("dbPass");
    pass.setAccessible(true);
    pass.set(opts, "");

    assertNotNull(dbMan.initDbConfig(opts));
  }

  @Test
  void makeJdbcUrl() {
    var opts = Options.getInstance();

    var test = mock(DbManager.class);
    when(test.makeJdbcUrl(any(SupportedPlatform.class), opts))
      .thenCallRealMethod();
    when(test.makeOracleJdbcUrl(opts)).thenReturn("foo");
    when(test.makePostgresJdbcUrl(opts)).thenReturn("bar");

    assertEquals("foo", test.makeJdbcUrl(SupportedPlatform.ORACLE, opts));
    assertEquals("bar", test.makeJdbcUrl(SupportedPlatform.POSTGRESQL, opts));
  }

  @Test
  void confErr() {
  }
}
