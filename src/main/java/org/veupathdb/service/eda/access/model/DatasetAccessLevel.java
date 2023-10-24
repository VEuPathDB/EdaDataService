package org.veupathdb.service.eda.access.model;

public enum DatasetAccessLevel {
  PUBLIC(true, true),
  CONTROLLED(true, false),
  PROTECTED(true, false),
  PRIVATE(false, false),
  PRERELEASE(false, false);

  private final boolean _allowsBasicAccess;
  private final boolean _allowsFullAccess;

  DatasetAccessLevel(boolean allowsBasicAccess, boolean allowsFullAccess) {
    _allowsBasicAccess = allowsBasicAccess;
    _allowsFullAccess = allowsFullAccess;
  }

  public boolean allowsBasicAccess() {
    return _allowsBasicAccess;
  }

  public boolean allowsFullAccess() {
    return _allowsFullAccess;
  }
}
