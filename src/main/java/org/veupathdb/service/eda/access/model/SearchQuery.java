package org.veupathdb.service.eda.access.model;

public class SearchQuery
{
  private String datasetId;

  private Long limit;

  private Long offset;

  private ApprovalStatus approvalStatus;

  public boolean hasDatasetId() {
    return datasetId != null;
  }

  public String getDatasetId() {
    return datasetId;
  }

  public SearchQuery setDatasetId(final String datasetId) {
    if (datasetId != null)
      this.datasetId = datasetId.isBlank() ? null : datasetId;
    else
      this.datasetId = null;

    return this;
  }

  public boolean hasLimit() {
    return limit != null;
  }

  public Long getLimit() {
    return limit;
  }

  public SearchQuery setLimit(final Long limit) {
    if (limit != null)
      this.limit = limit < 1 ? null : limit;
    else
      this.limit = null;

    return this;
  }

  public boolean hasOffset() {
    return offset != null;
  }

  public Long getOffset() {
    return offset;
  }

  public SearchQuery setOffset(final Long offset) {
    if (offset != null)
      this.offset = offset < 1 ? null : offset;
    else
      this.offset = null;

    return this;
  }

  public boolean hasApprovalStatus() {
    return approvalStatus != null;
  }

  public ApprovalStatus getApprovalStatus() {
    return approvalStatus;
  }

  public SearchQuery setApprovalStatus(final ApprovalStatus approvalStatus) {
    this.approvalStatus = approvalStatus;
    return this;
  }
}
