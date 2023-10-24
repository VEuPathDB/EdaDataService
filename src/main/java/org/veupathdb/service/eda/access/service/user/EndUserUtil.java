package org.veupathdb.service.eda.access.service.user;

import io.vulpine.lib.query.util.basic.BasicPreparedVoidQuery;
import org.apache.logging.log4j.Logger;
import org.veupathdb.lib.container.jaxrs.providers.LogProvider;
import org.veupathdb.service.eda.access.model.*;
import org.veupathdb.service.eda.access.model.ApprovalStatus;
import org.veupathdb.service.eda.access.model.RestrictionLevel;
import org.veupathdb.service.eda.access.repo.DB;
import org.veupathdb.service.eda.access.repo.SQL;
import org.veupathdb.service.eda.access.util.PsBuilder;
import org.veupathdb.service.eda.generated.model.EndUser;
import org.veupathdb.service.eda.generated.model.EndUserCreateRequest;
import org.veupathdb.service.eda.generated.model.EndUserImpl;
import org.veupathdb.service.eda.generated.model.EndUserList;
import org.veupathdb.service.eda.generated.model.EndUserListImpl;
import org.veupathdb.service.eda.generated.model.UserDetailsImpl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class EndUserUtil
{
  private static final Logger log = LogProvider.logger(EndUserUtil.class);

  /**
   * Parses a single {@link ResultSet} row into an instance of
   * {@link EndUserRow}.
   * <p>
   * Note, this method does not index the result set, {@link ResultSet#next()}
   * must be called before calling this method.
   *
   * @param rs Cursor to a query result row to parse into an {@code EndUserRow}.
   *
   * @return New {@code EndUserRow} parsed from the given result set.
   */
  static EndUserRow parseEndUserRow(final ResultSet rs) throws Exception {
    log.trace("EndUserRepo$Select#parseEndUserRow(ResultSet)");

    return (EndUserRow) new EndUserRow()
      .setEndUserID(rs.getLong(DB.Column.EndUser.EndUserID))
      .setAnalysisPlan(rs.getString(DB.Column.EndUser.AnalysisPlan))
      .setApprovalStatus(ApprovalStatusCache.getInstance()
        .get(rs.getShort(DB.Column.EndUser.ApprovalStatus))
        .orElseThrow())
      .setDatasetId(rs.getString(DB.Column.EndUser.DatasetId))
      .setDisseminationPlan(rs.getString(DB.Column.EndUser.DisseminationPlan))
      .setDuration(rs.getInt(DB.Column.EndUser.Duration))
      .setPriorAuth(rs.getString(DB.Column.EndUser.PriorAuth))
      .setPurpose(rs.getString(DB.Column.EndUser.Purpose))
      .setResearchQuestion(rs.getString(DB.Column.EndUser.ResearchQuestion))
      .setRestrictionLevel(RestrictionLevelCache.getInstance()
        .get(rs.getShort(DB.Column.EndUser.RestrictionLevel))
        .orElseThrow())
      .setStartDate(rs.getObject(DB.Column.EndUser.StartDate, OffsetDateTime.class))
      .setDenialReason(rs.getString(DB.Column.EndUser.DenialReason))
      .setDateDenied(rs.getObject(DB.Column.EndUser.DateDenied, OffsetDateTime.class))
      .setAllowSelfEdits(rs.getBoolean(DB.Column.EndUser.AllowSelfEdits))
      .setUserId(rs.getLong(DB.Column.EndUser.UserId))
      .setEmail(rs.getString(DB.Column.Accounts.Email))
      .setOrganization(rs.getString(DB.Column.Misc.Organization))
      .setFirstName(rs.getString(DB.Column.Misc.FirstName))
      .setLastName(rs.getString(DB.Column.Misc.LastName));
  }

  /**
   * Converts from generated to internal approval status enum type.
   *
   * @param status External approval status value to convert.
   *
   * @return Translated internal approval status value.
   */
  static ApprovalStatus convertApproval(
    final org.veupathdb.service.eda.generated.model.ApprovalStatus status
  ) {
    log.trace("EndUserService#convertApproval(ApprovalStatus)");

    return status == null
      ? null
      : switch (status) {
        case APPROVED -> ApprovalStatus.APPROVED;
        case DENIED -> ApprovalStatus.DENIED;
        case REQUESTED -> ApprovalStatus.REQUESTED;
        case UNREQUESTED -> null; // not relevant to backend
      };
  }

  /**
   * Converts from internal to generated approval status enum type.
   *
   * @param status Internal approval status value to convert.
   *
   * @return Translated external approval status value.
   */
  public static org.veupathdb.service.eda.generated.model.ApprovalStatus convertApproval(
    final ApprovalStatus status
  ) {
    log.trace("EndUserService#convertApproval(ApprovalStatus)");
    return status == null
      ? org.veupathdb.service.eda.generated.model.ApprovalStatus.UNREQUESTED
      : switch (status) {
        case APPROVED -> org.veupathdb.service.eda.generated.model.ApprovalStatus.APPROVED;
        case DENIED -> org.veupathdb.service.eda.generated.model.ApprovalStatus.DENIED;
        case REQUESTED -> org.veupathdb.service.eda.generated.model.ApprovalStatus.REQUESTED;
      };
  }

  /**
   * Converts from generated to internal restriction level enum type.
   *
   * @param level External restriction level value to convert.
   *
   * @return Translated internal restriction level value.
   */
  static RestrictionLevel convertRestriction(
    final org.veupathdb.service.eda.generated.model.RestrictionLevel level
  ) {
    log.trace("EndUserService#convertRestriction(RestrictionLevel)");
    return level == null
      ? null
      : switch (level) {
        case PRERELEASE -> RestrictionLevel.PRERELEASE;
        case CONTROLLED -> RestrictionLevel.CONTROLLED;
        case PRIVATE -> RestrictionLevel.PRIVATE;
        case PROTECTED -> RestrictionLevel.PROTECTED;
        case PUBLIC -> RestrictionLevel.PUBLIC;
      };
  }

  /**
   * Converts from internal to generated restriction level enum type.
   *
   * @param level Internal restriction level value to convert.
   *
   * @return Translated external restriction level value.
   */
  static org.veupathdb.service.eda.generated.model.RestrictionLevel convertRestriction(
    final RestrictionLevel level
  ) {
    log.trace("EndUserService#convertRestriction(RestrictionLevel)");
    return level == null
      ? null
      : switch (level) {
        case PRIVATE -> org.veupathdb.service.eda.generated.model.RestrictionLevel.PRIVATE;
        case CONTROLLED -> org.veupathdb.service.eda.generated.model.RestrictionLevel.CONTROLLED;
        case PRERELEASE -> org.veupathdb.service.eda.generated.model.RestrictionLevel.PRERELEASE;
        case PROTECTED -> org.veupathdb.service.eda.generated.model.RestrictionLevel.PROTECTED;
        case PUBLIC -> org.veupathdb.service.eda.generated.model.RestrictionLevel.PUBLIC;
      };
  }

  @SuppressWarnings("deprecation")
  static EndUserRow createRequest2EndUserRow(
    final EndUserCreateRequest req
  ) {
    log.trace("EndUserService#createRequest2EndUserRow(EndUserCreateRequest)");

    OffsetDateTime start = null;

    if (req.getStartDate() != null)
      start = req.getStartDate()
        .toInstant()
        .atOffset(ZoneOffset.ofHoursMinutes(
          req.getStartDate().getTimezoneOffset() / 60,
          req.getStartDate().getTimezoneOffset() % 60
        ));

    return (EndUserRow) new EndUserRow()
      .setDatasetId(req.getDatasetId())
      .setStartDate(start)
      .setDuration(req.getDuration())
      .setPurpose(req.getPurpose())
      .setResearchQuestion(req.getResearchQuestion())
      .setAnalysisPlan(req.getAnalysisPlan())
      .setDisseminationPlan(req.getDisseminationPlan())
      .setPriorAuth(req.getPriorAuth())
      .setRestrictionLevel(convertRestriction(req.getRestrictionLevel()))
      .setApprovalStatus(convertApproval(req.getApprovalStatus()))
      .setDenialReason(req.getDenialReason())
      .setUserId(req.getUserId());
  }

  /**
   * Converts a list of {@link EndUserRow} instances into an instance of the
   * generated type {@link EndUserList}.
   *
   * @param rows   List of rows to convert.
   * @param offset Pagination/row offset value.
   * @param total  Total number of possible results.
   *
   * @return converted end user list.
   */
  static EndUserList rows2EndUserList(
    final List<EndUserRow> rows,
    final long offset,
    final long total
  ) {
    log.trace("EndUserService#rows2EndUserList(List, int, int)");
    final var out = new EndUserListImpl();

    out.setOffset(offset);
    out.setTotal(total);
    out.setRows((long)rows.size());
    out.setData(rows.stream()
      .map(EndUserUtil::row2EndUser)
      .collect(Collectors.toList()));

    return out;
  }

  /**
   * Converts an {@link EndUserRow} instance into an instance of the generated
   * type {@link EndUser}.
   *
   * @param row end user data to convert
   *
   * @return converted end user data
   */
  static EndUser row2EndUser(final EndUserRow row) {
    log.trace("EndUserService#row2EndUser(EndUserRow)");

    final var user = new UserDetailsImpl();
    user.setUserId(row.getUserId());
    user.setLastName(row.getLastName());
    user.setFirstName(row.getFirstName());
    user.setOrganization(row.getOrganization());
    user.setEmail(row.getEmail());

    final var out = new EndUserImpl();
    out.setUser(user);
    out.setDatasetId(row.getDatasetId());
    out.setAnalysisPlan(row.getAnalysisPlan());
    out.setApprovalStatus(convertApproval(row.getApprovalStatus()));
    out.setDenialReason(row.getDenialReason());
    out.setDisseminationPlan(row.getDisseminationPlan());
    out.setDuration(row.getDuration());
    out.setPurpose(row.getPurpose());
    out.setResearchQuestion(row.getResearchQuestion());
    out.setRestrictionLevel(convertRestriction(row.getRestrictionLevel()));
    out.setStartDate(Date.from(row.getStartDate().toInstant()));
    out.setPriorAuth(row.getPriorAuth());
    out.setAllowEdit(row.isAllowSelfEdits());

    return out;
  }

  static String formatEndUserId(final long userId, final String datasetId) {
    log.trace("EndUserService#formatEndUserId(long, String)");

    return userId + "-" + datasetId;
  }

  /**
   * Inserts a new end user row modification record into the history table.
   *
   * @param con         Database connection to use for the record insertion.
   * @param row         Updated end user row to insert.
   * @param causeUserID User ID of the user who requested the end user
   *                    modification.
   */
  static void insertHistoryEvent(
    final Connection con,
    final HistoryAction action,
    final EndUserRow row,
    final long causeUserID
  ) throws Exception {
    new BasicPreparedVoidQuery(
      SQL.Insert.EndUserHistory,
      con,
      new PsBuilder()
        // 1
        .setLong(row.getEndUserID())
        .setLong(row.getUserId())
        .setString(row.getDatasetId())
        .setShort(RestrictionLevelCache.getInstance().get(row.getRestrictionLevel()).orElseThrow())
        // 5
        .setShort(ApprovalStatusCache.getInstance().get(row.getApprovalStatus()).orElseThrow())
        .setObject(row.getStartDate(), Types.DATE)
        .setLong(row.getDuration())
        .setString(row.getPurpose())
        .setString(row.getResearchQuestion())
        // 10
        .setString(row.getAnalysisPlan())
        .setString(row.getDisseminationPlan())
        .setString(row.getPriorAuth())
        .setString(row.getDenialReason())
        .setObject(row.getDateDenied(), Types.TIMESTAMP_WITH_TIMEZONE)
        // 15
        .setBoolean(row.isAllowSelfEdits())
        .setString(action.name())
        .setLong(causeUserID)
        ::build
    ).execute();
  }
}
