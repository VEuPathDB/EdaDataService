package org.veupathdb.service.eda.common.model;

public enum VariableSource {
  ID,
  NATIVE,
  INHERITED,
  DERIVED_TRANSFORM,
  DERIVED_REDUCTION,
  COMPUTED;

  public boolean isNativeOrId() {
    return equals(ID) || equals(NATIVE);
  }

  public boolean isResident() {
    return !equals(INHERITED);
  }
}
