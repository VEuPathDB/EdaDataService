package org.veupathdb.service.eda.access.service.dataset;

import io.vulpine.lib.query.util.basic.BasicPreparedReadQuery;
import org.veupathdb.service.eda.access.model.Dataset;
import org.veupathdb.service.eda.access.model.DatasetAccessLevel;
import org.veupathdb.service.eda.access.model.DatasetProps;
import org.veupathdb.service.eda.access.repo.DB;
import org.veupathdb.service.eda.access.repo.SQL;
import org.veupathdb.service.eda.access.service.QueryUtil;
import org.veupathdb.service.eda.access.util.SqlUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DatasetRepo
{
  public static final class Select
  {
    // mapped property -> pseudo column names in SQL
    static final String PROP_STUDY_ACCESS = "study_access";
    static final String PROP_DISPLAY_NAME = "display_name";
    static final String PROP_SHORT_DISPLAY_NAME = "short_display_name";
    static final String PROP_DESCRIPTION = "description";
    static final String PROP_DAYS_FOR_APPROVAL = "days_for_approval";
    static final String PROP_CUSTOM_APPROVAL_EMAIL_BODY = "custom_approval_email_body";

    static Select instance;

    public List<DatasetProps> datasetProps() throws Exception {
      final var sql = SQL.Select.Datasets.Access;

      try (
        final var cn = QueryUtil.appDbConnection();
        final var stmt = cn.createStatement();
        final var rs = QueryUtil.executeQueryLogged(stmt, sql);
      ) {
        List<DatasetProps> datasetProps = new ArrayList<>();
        while (rs.next()) {
          datasetProps.add(new DatasetProps(
            rs.getString(DB.Column.DatasetPresenters.DatasetId),
            rs.getString(DB.Column.StudyIdDatasetId.StudyId),
            rs.getString(DB.Column.DatasetPresenters.DatasetSha1Digest),
            Optional.ofNullable(rs.getString(PROP_STUDY_ACCESS))  // this value may be null
              .map(String::toUpperCase)                           // if not null, uppercase for enum conversion
              .map(DatasetAccessLevel::valueOf)                   // then convert to access level
              .orElse(DatasetAccessLevel.PUBLIC),                 // make public if this value does not exist
            rs.getString(PROP_DISPLAY_NAME),
            rs.getString(PROP_SHORT_DISPLAY_NAME),
            rs.getString(PROP_DESCRIPTION),
            rs.getString(PROP_CUSTOM_APPROVAL_EMAIL_BODY),
            Optional.ofNullable(rs.getString(PROP_DAYS_FOR_APPROVAL))
                .map(Long::parseLong)
                .orElse(null)
          ));
        }
        return datasetProps;
      }
    }

    /**
     * @param datasetId ID string for the dataset to check.
     *
     * @return whether or not the given datasetId points to an already existent
     * dataset.
     *
     * @throws Exception if a database error occurs while attempting to execute
     *                   this query.
     */
    public boolean getDatasetExists(final String datasetId) throws Exception {
      final var sql = SQL.Select.Datasets.Exists;

      try (
        final var cn = QueryUtil.appDbConnection();
        final var ps = QueryUtil.prepareStatement(cn, sql)
      ) {
        ps.setString(1, datasetId);

        try (final var rs = QueryUtil.executeQueryLogged(ps, sql)) {
          return rs.next();
        }
      }
    }

    public Optional<Dataset> selectDataset(final String datasetId) throws Exception {
      return new BasicPreparedReadQuery<>(
        SQL.Select.Datasets.ById,
        QueryUtil.getInstance()::getAppDbConnection,
        SqlUtil.optParser(DatasetUtil.getInstance()::resultSetToDataset),
        SqlUtil.prepareSingleString(datasetId)
      ).execute().getValue();
    }

    public static Select getInstance() {
      if (instance == null)
        instance = new Select();

      return instance;
    }

    public static boolean datasetExists(final String datasetId) throws Exception {
      return getInstance().getDatasetExists(datasetId);
    }

    public static Optional<Dataset> getDataset(final String datasetId) throws Exception {
      return getInstance().selectDataset(datasetId);
    }

    public static List<DatasetProps> getDatasetProps() throws Exception {
      return getInstance().datasetProps();
    }
  }
}
