package org.veupathdb.service.eda.access.model;

public class PartialProviderRow extends UserRow
{
  private String datasetId;
  private boolean isManager;

  public String getDatasetId() {
    return datasetId;
  }

  public void setDatasetId(String datasetId) {
    this.datasetId = datasetId;
  }

  public boolean isManager() {
    return isManager;
  }

  public void setManager(boolean manager) {
    isManager = manager;
  }
}
