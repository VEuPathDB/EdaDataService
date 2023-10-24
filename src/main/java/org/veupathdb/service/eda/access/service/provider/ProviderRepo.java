package org.veupathdb.service.eda.access.service.provider;

import io.vulpine.lib.query.util.basic.BasicPreparedListReadQuery;
import io.vulpine.lib.query.util.basic.BasicPreparedMapReadQuery;
import io.vulpine.lib.query.util.basic.BasicPreparedReadQuery;
import io.vulpine.lib.query.util.basic.BasicPreparedWriteQuery;
import org.veupathdb.service.eda.access.model.PartialProviderRow;
import org.veupathdb.service.eda.access.model.ProviderRow;
import org.veupathdb.service.eda.access.repo.DB;
import org.veupathdb.service.eda.access.repo.SQL;
import org.veupathdb.service.eda.access.service.QueryUtil;
import org.veupathdb.service.eda.access.util.PsBuilder;
import org.veupathdb.service.eda.access.util.SqlUtil;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProviderRepo
{
  public static class Delete
  {
    public static void byId(final long providerId) throws Exception {
      new BasicPreparedWriteQuery(
        SQL.Delete.Providers.ById,
        QueryUtil::acctDbConnection,
        SqlUtil.prepareSingleLong(providerId)
      ).execute();
    }
  }

  public static class Insert
  {
    public static long newProvider(final PartialProviderRow row) throws Exception {
      return QueryUtil.performInsertWithIdGeneration(
        SQL.Insert.Providers,
        QueryUtil::acctDbConnection,
        DB.Column.Provider.ProviderId,
        new PsBuilder()
          .setLong(row.getUserId())
          .setBoolean(row.isManager())
          .setString(row.getDatasetId())
          ::build
      );
    }
  }

  public static class Select
  {
    public static List<ProviderRow> byDataset(
      final String datasetId,
      final Long limit,
      final Long offset
    ) throws Exception {
      return new BasicPreparedListReadQuery<>(
        SQL.Select.Providers.ByDataset,
        QueryUtil::acctDbConnection,
        ProviderUtil.getInstance()::resultToProviderRow,
        new PsBuilder().setString(datasetId).setLong(offset).setLong(limit)::build
      ).execute().getValue();
    }

    public static Optional<ProviderRow> byId(final long providerId) throws Exception {
      return new BasicPreparedReadQuery<>(
        SQL.Select.Providers.ById,
        QueryUtil::acctDbConnection,
        SqlUtil.optParser(ProviderUtil.getInstance()::resultToProviderRow),
        SqlUtil.prepareSingleLong(providerId)
      ).execute().getValue();
    }

    public static Optional<ProviderRow> byUserAndDataset(final long userId, final String datasetId)
    throws Exception {
      return new BasicPreparedReadQuery<>(
        SQL.Select.Providers.ByUserDataset,
        QueryUtil::acctDbConnection,
        SqlUtil.optParser(ProviderUtil.getInstance()::resultToProviderRow),
        new PsBuilder().setLong(userId).setString(datasetId)::build
      ).execute().getValue();
    }

    public static List<ProviderRow> byUserId(final long userId) throws Exception {
      return new BasicPreparedListReadQuery<>(
        SQL.Select.Providers.ByUserId,
        QueryUtil::acctDbConnection,
        ProviderUtil.getInstance()::resultToProviderRow,
        SqlUtil.prepareSingleLong(userId)
      ).execute().getValue();
    }

    public static long countByDataset(final String datasetId) throws Exception {
      return new BasicPreparedReadQuery<>(
        SQL.Select.Providers.CountByDataset,
        QueryUtil::acctDbConnection,
        SqlUtil.reqParser(rs -> rs.getLong(1)),
        SqlUtil.prepareSingleString(datasetId)
      ).execute().getValue();
    }

    /**
     * Returns a map containing a set of dataset IDs the user is a provider for;
     * each mapped to a boolean flag indicating whether or not the given user is
     * marked as a manager for that dataset.
     *
     * If the given user id is not a provider for any datasets, the returned map
     * will be empty.
     *
     * @param userId ID of the user to lookup.
     *
     * @return the map described above.
     */
    public static Map<String, Boolean> datasets(final long userId) throws Exception {
      return new BasicPreparedMapReadQuery<>(
        SQL.Select.Providers.Datasets,
        QueryUtil::acctDbConnection,
        rs -> rs.getString(DB.Column.Provider.DatasetId),
        rs -> rs.getBoolean(DB.Column.Provider.IsManager),
        SqlUtil.prepareSingleLong(userId)
      ).execute().getValue();
    }
  } // End::Select

  public static class Update
  {
    static void isManagerById(final ProviderRow row) throws Exception {
      new BasicPreparedWriteQuery(
        SQL.Update.Providers.ById,
        QueryUtil::acctDbConnection,
        new PsBuilder().setBoolean(row.isManager()).setLong(row.getProviderId())::build
      ).execute();
    }
  }
}
