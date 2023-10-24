package org.veupathdb.service.eda.access.model;

import java.time.Duration;
import java.util.Optional;

/**
 * A representation of the dataset properties present in tuning tables.
 */
public class DatasetProps {

  public final String datasetId;
  public final String studyId;
  public final String sha1hash;
  public final DatasetAccessLevel accessLevel;
  public final String displayName;
  public final String shortDisplayName;
  public final String description;
  public final Duration durationForApproval;
  public final String customApprovalEmailBody;

  public DatasetProps(
      final String datasetId,
      final String studyId,
      final String sha1hash,
      final DatasetAccessLevel accessLevel,
      final String displayName,
      final String shortDisplayName,
      final String description,
      final String customApprovalEmailBody,
      final Long daysForApproval) {
    this.datasetId = datasetId;
    this.studyId = studyId;
    this.sha1hash = sha1hash;
    this.accessLevel = accessLevel;
    this.displayName = displayName;
    this.shortDisplayName = shortDisplayName;
    this.description = description;
    this.customApprovalEmailBody = customApprovalEmailBody;
    this.durationForApproval = Optional.ofNullable(daysForApproval)
        .map(Duration::ofDays)
        .orElse(null);
  }

}
