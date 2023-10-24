package org.veupathdb.service.eda.access.service.account;

import io.vulpine.lib.query.util.basic.BasicPreparedReadQuery;
import org.apache.logging.log4j.Logger;
import org.veupathdb.lib.container.jaxrs.providers.LogProvider;
import org.veupathdb.service.eda.access.repo.SQL;
import org.veupathdb.service.eda.access.service.QueryUtil;
import org.veupathdb.service.eda.access.util.SqlUtil;

import java.util.Optional;

public final class AccountRepo
{
  public static class Select
  {
    private static Select instance;

    private final Logger log;

    public Select() {
      log = LogProvider.logger(getClass());
    }

    /**
     * Attempts to lookup a user's ID based on the given email address.
     *
     * @param email email to lookup.
     *
     * @return An option that will contain the user id attached to the given
     *         email if any such record was found.
     */
    public Optional<Long> selectUserIdByEmail(final String email) throws Exception {
      log.trace("AccountRepo$Select#selectUserIdByEmail(String)");

      return new BasicPreparedReadQuery<>(
        SQL.Select.Accounts.IdByEmail,
        QueryUtil.getInstance()::getAcctDbConnection,
        SqlUtil.optParser(SqlUtil::parseSingleLong),
        SqlUtil.prepareSingleString(email)
      ).execute().getValue();
    }

    public Optional<String> selectEmailByUserId(final long userId) throws Exception {
      return new BasicPreparedReadQuery<>(
          SQL.Select.Accounts.EmailById,
          QueryUtil.getInstance()::getAcctDbConnection,
          SqlUtil.optParser(SqlUtil::parseSingleString),
          SqlUtil.prepareSingleLong(userId)
      ).execute().getValue();
    }

    public static Select getInstance() {
      if (instance == null)
        instance = new Select();

      return instance;
    }

    public static boolean userExists(final long userId) throws Exception {
      final var sql = SQL.Select.Accounts.Exists;
      try (
        final var cn = QueryUtil.acctDbConnection();
        final var ps = QueryUtil.prepareStatement(cn, sql)
      ) {
        ps.setLong(1, userId);
        try (final var rs = QueryUtil.executeQueryLogged(ps, sql)) {
          return rs.next();
        }
      }
    }
  }
}
