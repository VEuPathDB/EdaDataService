package org.veupathdb.service.eda.access.service.staff;

import io.vulpine.lib.query.util.basic.BasicPreparedListReadQuery;
import io.vulpine.lib.query.util.basic.BasicPreparedReadQuery;
import io.vulpine.lib.query.util.basic.BasicPreparedWriteQuery;
import io.vulpine.lib.query.util.basic.BasicStatementReadQuery;
import org.veupathdb.service.eda.access.model.PartialStaffRow;
import org.veupathdb.service.eda.access.model.StaffRow;
import org.veupathdb.service.eda.access.repo.DB;
import org.veupathdb.service.eda.access.repo.SQL;
import org.veupathdb.service.eda.access.service.QueryUtil;
import org.veupathdb.service.eda.access.util.PsBuilder;
import org.veupathdb.service.eda.access.util.SqlUtil;

import java.util.List;
import java.util.Optional;

public class StaffRepo
{
  public interface Delete
  {
    static void byId(final Long staffId) throws Exception {
      new BasicPreparedWriteQuery(
        SQL.Delete.Staff.ById,
        QueryUtil.getInstance()::getAcctDbConnection,
        SqlUtil.prepareSingleLong(staffId)
      ).execute();
    }
  }

  public interface Insert
  {
    static long newStaff(final PartialStaffRow row) throws Exception {
      return QueryUtil.performInsertWithIdGeneration(
        SQL.Insert.Staff,
        QueryUtil.getInstance()::getAcctDbConnection,
        DB.Column.Staff.StaffId,
        new PsBuilder()
          .setLong(row.getUserId())
          .setBoolean(row.isOwner())
          ::build
      );
    }
  }

  public interface Select
  {
    static Optional<StaffRow> byId(final Long staffId) throws Exception {
      return new BasicPreparedReadQuery<>(
        SQL.Select.Staff.ById,
        QueryUtil.getInstance()::getAcctDbConnection,
        SqlUtil.optParser(StaffUtil.getInstance()::resultRowToStaffRow),
        SqlUtil.prepareSingleLong(staffId)
      ).execute().getValue();
    }

    static Optional<StaffRow> byUserId(final long userId) throws Exception {
      return new BasicPreparedReadQuery<>(
        SQL.Select.Staff.ByUserId,
        QueryUtil.getInstance()::getAcctDbConnection,
        SqlUtil.optParser(StaffUtil.getInstance()::resultRowToStaffRow),
        SqlUtil.prepareSingleLong(userId)
      ).execute().getValue();
    }

    static int count() throws Exception {
      return new BasicStatementReadQuery<>(
        SQL.Select.Staff.CountAll,
        QueryUtil.getInstance()::getAcctDbConnection,
        SqlUtil.reqParser(SqlUtil::parseSingleInt)
      ).execute().getValue();
    }

    static List<StaffRow> list(final long limit, final long offset) throws Exception {
      return new BasicPreparedListReadQuery<>(
        SQL.Select.Staff.All,
        QueryUtil.getInstance()::getAcctDbConnection,
        StaffUtil.getInstance()::resultRowToStaffRow,
        new PsBuilder().setLong(offset).setLong(limit)::build
      ).execute().getValue();
    }
  } // End::Select

  public interface Update
  {
    static void ownerFlagById(final StaffRow row) throws Exception {
      new BasicPreparedWriteQuery(
        SQL.Update.Staff.ById,
        QueryUtil.getInstance()::getAcctDbConnection,
        new PsBuilder().setBoolean(row.isOwner()).setLong(row.getStaffId())::build
      ).execute();
    }
  }
}
