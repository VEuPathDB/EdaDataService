package org.veupathdb.service.eda.access.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ApprovalStatusCache
{
  private static final ApprovalStatusCache instance = new ApprovalStatusCache();

  private final Map < Short, ApprovalStatus > byId;
  private final Map < ApprovalStatus, Short > byValue;

  private ApprovalStatusCache() {
    byId = new HashMap <>();
    byValue = new HashMap <>();
  }

  public Optional < ApprovalStatus > get(short id) {
    return Optional.ofNullable(byId.get(id));
  }

  public Optional < Short > get(ApprovalStatus status) {
    return Optional.ofNullable(byValue.get(status));
  }

  public void put(short id, ApprovalStatus status) {
    byId.put(id, Objects.requireNonNull(status));
    byValue.put(status, id);
  }

  public static ApprovalStatusCache getInstance() {
    return instance;
  }
}
