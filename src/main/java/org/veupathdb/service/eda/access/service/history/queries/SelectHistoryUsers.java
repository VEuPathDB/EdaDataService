package org.veupathdb.service.eda.access.service.history.queries;

import io.vulpine.lib.query.util.basic.BasicStatementMapReadQuery;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.veupathdb.service.eda.access.service.history.model.HistoryUserRow;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SelectHistoryUsers
{
  private static final Logger Log = LogManager.getLogger(SelectHistoryUsers.class);

  private static final String SQLPrefix = """
WITH
  end_user_ids AS (
""";
  private static final String SubPrefix = "    SELECT ";
  private static final String SubSuffix = " AS end_user_id FROM dual\n";
  private static final String Union     = "    UNION\n";
  private static final String SQLSuffix = """
  )
, user_ids AS (
    SELECT
      user_id AS user_id
    FROM
      studyaccess.end_user_history
    WHERE
      end_user_id IN (SELECT * FROM end_user_ids)
    UNION
    SELECT
      history_cause_user AS user_id
    FROM
      studyaccess.end_user_history
    WHERE
      end_user_id IN (SELECT * FROM end_user_ids)
  )
SELECT
  (
    SELECT
      value
    FROM
      useraccounts.account_properties
    WHERE
        user_id = b.user_id
    AND key = 'first_name'
  )
, (
    SELECT
      value
    FROM
      useraccounts.account_properties
    WHERE
        user_id = b.user_id
    AND key = 'last_name'
  )
, (
    SELECT
      value
    FROM
      useraccounts.account_properties
    WHERE
        user_id = b.user_id
    AND key = 'organization'
  )
, (
    SELECT
      email
    FROM
      useraccounts.accounts
    WHERE
      user_id = b.user_id
  )
, user_id
FROM
  user_ids b
""";

  private final Connection con;
  private final List<Long> endUserIDs;

  public SelectHistoryUsers(Connection con, List<Long> endUserIDs) {
    Log.trace("::new(con={}, endUserIDs={})", con, endUserIDs);

    this.con        = con;
    this.endUserIDs = endUserIDs;
  }

  public Map<Long, HistoryUserRow> run() throws Exception {
    Log.trace("#run()");

    if (endUserIDs.isEmpty())
      return Collections.emptyMap();

    var query = assembleQuery();

//    Log.debug("Running query: {}", query);

    return new BasicStatementMapReadQuery<>(
      query,
      con,
      rs -> rs.getLong(5),
      HistoryUserRow::new
    )
      .execute()
      .getValue();
  }

  private String assembleQuery() {
    var sql = new StringBuilder(SQLPrefix);

    var first = true;
    for (var id : endUserIDs) {
      if (!first) {
        sql.append(Union);
      } else {
        first = false;
      }

      sql.append(SubPrefix).append(id).append(SubSuffix);
    }

    return sql.append(SQLSuffix).toString();
  }
}
