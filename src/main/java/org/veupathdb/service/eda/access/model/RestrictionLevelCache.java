package org.veupathdb.service.eda.access.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class RestrictionLevelCache
{
  private static final RestrictionLevelCache instance = new RestrictionLevelCache();

  private final Map < Short, RestrictionLevel > byId;
  private final Map < RestrictionLevel, Short > byValue;

  private RestrictionLevelCache() {
    byId = new HashMap <>();
    byValue = new HashMap <>();
  }

  public Optional < RestrictionLevel > get(short id) {
    return Optional.ofNullable(byId.get(id));
  }

  public Optional < Short > get(RestrictionLevel status) {
    return Optional.ofNullable(byValue.get(status));
  }

  public void put(short id, RestrictionLevel status) {
    byId.put(id, Objects.requireNonNull(status));
    byValue.put(status, id);
  }

  public static RestrictionLevelCache getInstance() {
    return instance;
  }
}
