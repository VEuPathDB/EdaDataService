package org.veupathdb.service.eda.access.service.history.queries;

import io.vulpine.lib.query.util.basic.BasicPreparedListReadQuery;
import org.veupathdb.service.eda.access.service.history.model.HistoryResultRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Objects;

/**
 * Fetches history rows that were created by the dataset manager with the given
 * user ID.
 */
public class SelectUserHistoryRows
{
  private static final String SQL = "SELECT\n"
    + "  end_user_id\n"
    + ", a.user_id\n"
    + ", dataset_presenter_id\n"
    + ", history_action\n"
    + ", history_timestamp\n"
    + ", history_cause_user\n"
    + ", restriction_level_id\n"
    + ", approval_status_id\n"
    + ", start_date\n"
    + ", duration\n"
    + ", purpose\n"
    + ", research_question\n"
    + ", analysis_plan\n"
    + ", dissemination_plan\n"
    + ", prior_auth\n"
    + ", denial_reason\n"
    + ", date_denied\n"
    + ", allow_self_edits\n"
    + "FROM\n"
    + "  studyaccess.end_user_history a\n"
    + "  INNER JOIN studyaccess.providers b\n"
    + "    ON b.dataset_id = a.dataset_presenter_id\n"
    + "    AND b.user_id = a.history_cause_user\n"
    + "WHERE\n"
    + "  b.user_id = ?\n"
    + "  AND b.is_manager = 1\n"
    + "OFFSET ? ROWS\n"
    + "FETCH FIRST ? ROWS ONLY";

  private static final int HardLimit = 100;

  private final Connection con;
  private final long userID;
  private final long limit;
  private final long offset;

  public SelectUserHistoryRows(Connection con, long userID, long limit, long offset) {
    this.con    = Objects.requireNonNull(con);
    this.userID = userID;
    this.limit  = Math.min(Math.max(1, limit), HardLimit);
    this.offset = Math.max(0, offset);
  }

  public List<HistoryResultRow> run() throws Exception {
    return new BasicPreparedListReadQuery<>(SQL, con, HistoryResultRow::new, this::prep)
      .execute()
      .getValue();
  }

  private void prep(PreparedStatement ps) throws Exception {
    ps.setLong(1, userID);
    ps.setLong(2, offset);
    ps.setLong(3, limit);
  }
}
