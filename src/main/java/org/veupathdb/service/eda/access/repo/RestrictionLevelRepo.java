package org.veupathdb.service.eda.access.repo;

import org.veupathdb.service.eda.access.model.RestrictionLevel;
import org.veupathdb.service.eda.access.model.RestrictionLevelCache;
import org.veupathdb.service.eda.access.service.QueryUtil;

public final class RestrictionLevelRepo
{
  public interface Select
  {
    static void populateRestrictionLevelCache() throws Exception {
      try (
        final var cn = QueryUtil.acctDbConnection();
        final var st = cn.createStatement();
        final var rs = QueryUtil.executeQueryLogged(st, SQL.Select.Enums.RestrictionLevel)
      ) {
        final var cache = RestrictionLevelCache.getInstance();

        while (rs.next()) {
          cache.put(rs.getShort(1), RestrictionLevel.valueOf(rs.getString(2)
            .toUpperCase()));
        }
      }
    }
  }
}
