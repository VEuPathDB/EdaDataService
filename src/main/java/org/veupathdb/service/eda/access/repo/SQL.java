package org.veupathdb.service.eda.access.repo;

import io.vulpine.lib.sql.load.SqlLoader;
import org.veupathdb.service.eda.access.repo.DB.Table;

import java.util.function.Supplier;

public interface SQL
{
  SqlLoader loader = new SqlLoader();

  enum Method
  {
    SELECT,
    UPDATE,
    INSERT,
    DELETE
  }

  interface Delete
  {
    interface EndUsers
    {
      String ById = delete(Table.EndUsers, "by-id");
    }

    interface Providers
    {
      String ById = delete(Table.Providers, "by-id");
    }

    interface Staff
    {
      String ById = delete(Table.Staff, "by-id");
    }
  }

  interface Insert
  {
    String
      EndUser        = insert(Table.EndUsers, "insert"),
      EndUserHistory = insert(Table.EndUserHistory, "insert"),
      Providers      = insert(Table.Providers, "insert"),
      Staff          = insert(Table.Staff, "insert");
  }

  interface Select
  {

    interface Accounts
    {
      String
        Exists    = select(Table.Accounts, "exists-by-id"),
        IdByEmail = select(Table.Accounts, "id-by-email"),
        EmailById = select(Table.Accounts, "email-by-id");
    }

    interface EndUsers
    {
      String
        ById                   = select(Table.EndUsers, "by-id"),
        CountByDataset         = select(Table.EndUsers, "count-by-dataset"),
        CountByDatasetFiltered = select(Table.EndUsers, "count-by-dataset-filtered"),
        CountByQuery           = select(Table.EndUsers, "count-by-query"),
        ByDataset              = select(Table.EndUsers, "by-dataset"),
        ByDatasetFiltered      = select(Table.EndUsers, "by-dataset-filtered"),
        ByQuery                = select(Table.EndUsers, "by-query"),
        Datasets               = select(Table.EndUsers, "datasets");
    }

    interface Providers
    {
      String
        ByDataset      = select(Table.Providers, "by-dataset"),
        ById           = select(Table.Providers, "by-id"),
        CountByDataset = select(Table.Providers, "count-by-dataset"),
        ByUserId       = select(Table.Providers, "by-user-id"),
        ByUserDataset  = select(Table.Providers, "by-user-id-and-dataset"),
        Datasets       = select(Table.Providers, "datasets");
    }

    interface Staff
    {
      String
        All      = select(Table.Staff, "all"),
        ById     = select(Table.Staff, "by-id"),
        ByUserId = select(Table.Staff, "by-user-id"),
        CountAll = select(Table.Staff, "count");
    }

    interface Datasets
    {
      String
        Exists = select(Table.Datasets, "exists"),
        ById   = select(Table.Datasets, "by-id"),
        Emails = select(Table.DatasetProperties, "emails"),
        Access = select(Table.DatasetProperties, "access");
    }

    interface UserDatasets
    {
      String
        ByUserAccess = select(Table.UserDatasetAttributes, "by-user-access"),
        ByDatasetId = select(Table.UserDatasetAttributes, "by-dataset-id");
    }

    interface Enums
    {
      String
        ApprovalStatus   = select(Table.ApprovalStatus, "all"),
        RestrictionLevel = select(Table.RestrictionLevel, "all");
    }
  }

  interface Update
  {
    interface Providers
    {
      String ById = update(Table.Providers, "by-id");
    }

    interface Staff
    {
      String ById = update(Table.Staff, "by-id");
    }

    interface EndUser
    {
      String
        ModUpdate  = update(Table.EndUsers, "mod-update"),
        SelfUpdate = update(Table.EndUsers, "self-update");
    }
  }

  private static String delete(final String path, final String query) {
    return loader.delete(path + "." + query)
      .orElseThrow(makeErr(Method.DELETE, path, query));
  }

  private static String insert(final String path, final String query) {
    return loader.insert(path + "." + query)
      .orElseThrow(makeErr(Method.INSERT, path, query));
  }

  private static String select(final String path, final String query) {
    return loader.select(path + "." + query)
      .orElseThrow(makeErr(Method.SELECT, path, query));
  }

  private static String update(final String path, final String query) {
    return loader.udpate(path + "." + query)
      .orElseThrow(makeErr(Method.UPDATE, path, query));
  }

  private static Supplier<RuntimeException> makeErr(
    final Method method,
    final String path,
    final String query
  ) {
    return () -> new RuntimeException("Failed to load " + method.name() +
      " query sql/" + path.replace(".", "/") + "/" + query);
  }
}
