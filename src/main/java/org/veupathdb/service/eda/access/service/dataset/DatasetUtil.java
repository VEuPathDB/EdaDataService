package org.veupathdb.service.eda.access.service.dataset;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.logging.log4j.Logger;
import org.veupathdb.lib.container.jaxrs.providers.LogProvider;
import org.veupathdb.service.eda.access.model.Dataset;
import org.veupathdb.service.eda.access.repo.DB.Column;
import org.veupathdb.service.eda.access.util.Format;

import java.sql.ResultSet;
import java.util.Map;

public class DatasetUtil
{
  private static DatasetUtil instance;

  private final Logger log;

  public DatasetUtil() {
    log = LogProvider.logger(getClass());
  }

  public Dataset resultSetToDataset(final ResultSet rs) throws Exception {
   return new Dataset()
      .setDatasetId(rs.getString(Column.DatasetPresenters.DatasetId))
      .setName(rs.getString(Column.DatasetPresenters.Name))
      .setDatasetNamePattern(rs.getString(Column.DatasetPresenters.DatasetNamePattern))
      .setDisplayName(rs.getString(Column.DatasetPresenters.DisplayName))
      .setShortDisplayName(rs.getString(Column.DatasetPresenters.ShortDisplayName))
      .setShortAttribution(rs.getString(Column.DatasetPresenters.ShortAttribution))
      // summary
      // protocol
      // description
      // usage
      // caveat
      // acknowledgement
      // release policy
      .setDisplayCategory(rs.getString(Column.DatasetPresenters.DisplayCategory))
      .setType(rs.getString(Column.DatasetPresenters.Type))
      .setSubtype(rs.getString(Column.DatasetPresenters.Subtype))
      .setCategory(rs.getString(Column.DatasetPresenters.Category))
      .setSpeciesScope(rs.getBoolean(Column.DatasetPresenters.IsSpeciesScope))
      .setBuildNumberIntroduced(rs.getInt(Column.DatasetPresenters.BuildNumberIntroduced))
      .setDatasetSha1Digest(rs.getString(Column.DatasetPresenters.DatasetSha1Digest))
      .putProperties(Format.Json.readValue(
        rs.getString(Column.Misc.Properties),
        new TypeReference<Map<Dataset.Property, String>>(){}));
  }

  public static DatasetUtil getInstance() {
    if (instance == null)
      instance = new DatasetUtil();

    return instance;
  }
}
