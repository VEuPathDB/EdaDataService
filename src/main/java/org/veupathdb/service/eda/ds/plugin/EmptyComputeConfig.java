package org.veupathdb.service.eda.ds.plugin;

import org.veupathdb.service.eda.generated.model.ComputeConfigBase;

import java.util.Map;

public class EmptyComputeConfig implements ComputeConfigBase {

  @Override
  public String getOutputEntityId() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setOutputEntityId(String outputEntityId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, Object> getAdditionalProperties() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setAdditionalProperties(String key, Object value) {
    throw new UnsupportedOperationException();
  }
}
