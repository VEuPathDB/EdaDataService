package org.veupathdb.service.eda.access.service.history.model;

import org.veupathdb.service.eda.access.model.*;

import java.sql.ResultSet;
import java.time.OffsetDateTime;

public class HistoryResultRow
{
  public final long             endUserID;
  public final long             userID;
  public final String           datasetPresenterID;
  public final HistoryAction    historyAction;
  public final OffsetDateTime   historyTimestamp;
  public final long             historyCauseUser;
  public final RestrictionLevel restrictionLevel;
  public final ApprovalStatus   approvalStatus;
  public final OffsetDateTime   startDate;
  public final long             duration;
  public final String           purpose;
  public final String           researchQuestion;
  public final String           analysisPlan;
  public final String           disseminationPlan;
  public final String           priorAuth;
  public final String           denialReason;
  public final OffsetDateTime   dateDenied;
  public final boolean          allowSelfEdits;

  public HistoryResultRow(ResultSet rs) throws Exception {
    endUserID          = rs.getLong(1);
    userID             = rs.getLong(2);
    datasetPresenterID = rs.getString(3);
    historyAction      = HistoryAction.valueOf(rs.getString(4));
    historyTimestamp   = rs.getObject(5, OffsetDateTime.class);
    historyCauseUser   = rs.getLong(6);
    restrictionLevel   = RestrictionLevelCache.getInstance().get(rs.getShort(7)).orElseThrow();
    approvalStatus     = ApprovalStatusCache.getInstance().get(rs.getShort(8)).orElseThrow();
    startDate          = rs.getObject(9, OffsetDateTime.class);
    duration           = rs.getLong(10);
    purpose            = rs.getString(11);
    researchQuestion   = rs.getString(12);
    analysisPlan       = rs.getString(13);
    disseminationPlan  = rs.getString(14);
    priorAuth          = rs.getString(15);
    denialReason       = rs.getString(16);
    dateDenied         = rs.getObject(17, OffsetDateTime.class);
    allowSelfEdits     = rs.getBoolean(18);
  }
}
