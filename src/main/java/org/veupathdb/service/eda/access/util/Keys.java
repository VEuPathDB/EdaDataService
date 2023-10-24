package org.veupathdb.service.eda.access.util;

public interface Keys
{
  interface Json
  {
    String
      KEY_ANALYSIS_PLAN      = "analysisPlan",
      KEY_APPROVAL_STATUS    = "approvalStatus",
      KEY_DATASET_ID         = "datasetId",
      KEY_DENIAL_REASON      = "denialReason",
      KEY_DISSEMINATION_PLAN = "disseminationPlan",
      KEY_DURATION           = "duration",
      KEY_EMAIL              = "email",
      KEY_IS_MANAGER         = "isManager",
      KEY_IS_OWNER           = "isOwner",
      KEY_PRIOR_AUTH         = "priorAuth",
      KEY_PURPOSE            = "purpose",
      KEY_RESEARCH_QUESTION  = "researchQuestion",
      KEY_RESTRICTION_LEVEL  = "restrictionLevel",
      KEY_START_DATE         = "startDate",
      KEY_USER               = "user",
      KEY_USER_ID            = "userId";

    // JSON Patch
    String
      KEY_FROM  = "from",
      KEY_OP    = "op",
      KEY_PATH  = "path",
      KEY_VALUE = "value";
  }
}
