package org.veupathdb.service.eda.access.model;

import java.time.OffsetDateTime;

/**
 * Database representation of an "EndUser". The name EndUser is slightly misleading as this entity represents the access
 * relationship between a user and a dataset. This might be a pending, failed or approved access request.
 */
public class EndUserRow extends UserRow
{
  private long             endUserID;
  private String           datasetId;
  private OffsetDateTime   startDate;
  private long             duration;
  private RestrictionLevel restrictionLevel;
  private String           purpose;
  private String           researchQuestion;
  private String           analysisPlan;
  private String           disseminationPlan;
  private ApprovalStatus   approvalStatus;
  private String           priorAuth;
  private String           denialReason;
  private OffsetDateTime   dateDenied;
  private boolean          allowSelfEdits;

  public long getEndUserID() {
    return endUserID;
  }

  public EndUserRow setEndUserID(long endUserID) {
    this.endUserID = endUserID;
    return this;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public EndUserRow setDatasetId(String datasetId) {
    this.datasetId = datasetId;
    return this;
  }

  public OffsetDateTime getStartDate() {
    return startDate;
  }

  public EndUserRow setStartDate(OffsetDateTime startDate) {
    this.startDate = startDate;
    return this;
  }

  public long getDuration() {
    return duration;
  }

  public EndUserRow setDuration(long duration) {
    this.duration = duration;
    return this;
  }

  public RestrictionLevel getRestrictionLevel() {
    return restrictionLevel;
  }

  public EndUserRow setRestrictionLevel(RestrictionLevel restrictionLevel) {
    this.restrictionLevel = restrictionLevel;
    return this;
  }

  public String getPurpose() {
    return purpose;
  }

  public EndUserRow setPurpose(String purpose) {
    this.purpose = purpose;
    return this;
  }

  public String getResearchQuestion() {
    return researchQuestion;
  }

  public EndUserRow setResearchQuestion(String researchQuestion) {
    this.researchQuestion = researchQuestion;
    return this;
  }

  public String getAnalysisPlan() {
    return analysisPlan;
  }

  public EndUserRow setAnalysisPlan(String analysisPlan) {
    this.analysisPlan = analysisPlan;
    return this;
  }

  public String getDisseminationPlan() {
    return disseminationPlan;
  }

  public EndUserRow setDisseminationPlan(String disseminationPlan) {
    this.disseminationPlan = disseminationPlan;
    return this;
  }

  public ApprovalStatus getApprovalStatus() {
    return approvalStatus;
  }

  public EndUserRow setApprovalStatus(ApprovalStatus approvalStatus) {
    this.approvalStatus = approvalStatus;
    return this;
  }

  public String getPriorAuth() {
    return priorAuth;
  }

  public EndUserRow setPriorAuth(String priorAuth) {
    this.priorAuth = priorAuth;
    return this;
  }

  public String getDenialReason() {
    return denialReason;
  }

  public EndUserRow setDenialReason(String denialReason) {
    this.denialReason = denialReason;
    return this;
  }

  public OffsetDateTime getDateDenied() {
    return dateDenied;
  }

  public EndUserRow setDateDenied(OffsetDateTime dateDenied) {
    this.dateDenied = dateDenied;
    return this;
  }

  public boolean isAllowSelfEdits() {
    return allowSelfEdits;
  }

  public EndUserRow setAllowSelfEdits(boolean allowSelfEdits) {
    this.allowSelfEdits = allowSelfEdits;
    return this;
  }
}
