package org.veupathdb.service.eda.access.service.history;

import jakarta.ws.rs.InternalServerErrorException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.veupathdb.lib.container.jaxrs.utils.db.DbManager;
import org.veupathdb.service.eda.generated.model.*;
import org.veupathdb.service.eda.access.service.history.model.HistoryResultRow;
import org.veupathdb.service.eda.access.service.history.model.HistoryUserRow;
import org.veupathdb.service.eda.access.service.history.queries.SelectHistoryRows;
import org.veupathdb.service.eda.access.service.history.queries.SelectHistoryUsers;
import org.veupathdb.service.eda.access.service.history.queries.SelectUserHistoryRows;
import org.veupathdb.service.eda.access.service.staff.StaffService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HistoryService
{
  private static final Logger Log = LogManager.getLogger(HistoryService.class);

  public static HistoryResponse getHistory(long userID, Long limit, Long offset) {
    Log.trace("::getHistory(userID={}, limit={}, offset={})", userID, limit, offset);

    List<HistoryResultRow>    rows;
    Map<Long, HistoryUserRow> users;

    try {
      try (var con = DbManager.accountDatabase().getDataSource().getConnection()) {

        // If the user is marked as an owner, load the full history.
        // Else load only the history rows for datasets the user is a manager of
        // (which may be 0 rows).
        if (StaffService.userIsOwner(userID)) {
          rows = new SelectHistoryRows(con, limit, offset).run();
        } else {
          rows = new SelectUserHistoryRows(con, userID, limit, offset).run();
        }

        var endUserIDs = rows.stream()
          .map(r -> r.endUserID)
          .distinct()
          .collect(Collectors.toList());

        users = new SelectHistoryUsers(con, endUserIDs).run();
      }

      var meta = new HistoryMetaImpl();
      meta.setOffset(offset);
      meta.setRows((long)rows.size());

      var out = new HistoryResponseImpl();
      out.setResults(new ArrayList<>());
      out.setMeta(meta);


      for (var row : rows) {
        var causeUser = users.get(row.historyCauseUser);
        if (causeUser == null) {
          Log.warn("Missing user entry for cause user {}.", row.historyCauseUser);
          continue;
        }

        var rowUser = users.get(row.userID);
        if (rowUser == null) {
          Log.warn("Missing user entry for row user {}.", row.userID);
          continue;
        }

        var res = new HistoryResultImpl();
        res.setCause(convertCause(row, causeUser));
        res.setRow(convertRow(row, rowUser));

        out.getResults().add(res);
      }

      return out;
    } catch (Exception e) {
      throw new InternalServerErrorException(e);
    }
  }

  private static HistoryCause convertCause(HistoryResultRow row, HistoryUserRow user) {
    var out = new HistoryCauseImpl();

    out.setAction(HistoryCause.ActionType.values()[row.historyAction.ordinal()]);
    out.setTimestamp(convert(row.historyTimestamp));
    out.setUser(convert(user));

    return out;
  }

  private static HistoryRow convertRow(HistoryResultRow row, HistoryUserRow user) {
    var out = new HistoryRowImpl();

    out.setEndUserID(row.endUserID);
    out.setUser(convert(user));
    out.setDatasetPresenterID(row.datasetPresenterID);
    out.setRestrictionLevel(HistoryRow.RestrictionLevelType.valueOf(row.restrictionLevel.name()));
    out.setApprovalStatus(HistoryRow.ApprovalStatusType.valueOf(row.approvalStatus.name()));
    out.setStartDate(convert(row.startDate));
    out.setDuration(row.duration);
    out.setPurpose(row.purpose);
    out.setResearchQuestion(row.researchQuestion);
    out.setAnalysisPlan(row.analysisPlan);
    out.setPriorAuth(row.priorAuth);
    out.setDenialReason(row.denialReason);
    out.setDateDenied(convert(row.dateDenied));
    out.setAllowSelfEdits(row.allowSelfEdits);

    return out;
  }

  private static HistoryUser convert(HistoryUserRow row) {
    var out = new HistoryUserImpl();

    out.setUserID(row.userID);
    out.setFirstName(row.firstName);
    out.setLastName(row.lastName);
    out.setEmail(row.email);
    out.setOrganization(row.organization);

    return out;
  }

  private static Date convert(OffsetDateTime odt) {
    return odt == null ? null : Date.from(odt.toInstant());
  }
}
