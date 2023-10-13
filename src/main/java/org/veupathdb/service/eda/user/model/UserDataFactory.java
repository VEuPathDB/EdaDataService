package org.veupathdb.service.eda.user.model;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.ArrayUtil;
import org.gusdb.fgputil.db.runner.BasicArgumentBatch;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.functional.FunctionalInterfaces.SupplierWithException;
import org.json.JSONObject;
import org.veupathdb.lib.container.jaxrs.model.User;
import org.veupathdb.service.eda.Resources;
import org.veupathdb.service.eda.generated.model.AnalysisDescriptor;
import org.veupathdb.service.eda.generated.model.AnalysisProvenance;
import org.veupathdb.service.eda.generated.model.AnalysisProvenanceImpl;
import org.veupathdb.service.eda.generated.model.AnalysisSummary;
import org.veupathdb.service.eda.generated.model.AnalysisSummaryImpl;
import org.veupathdb.service.eda.generated.model.AnalysisSummaryWithUser;
import org.veupathdb.service.eda.generated.model.AnalysisSummaryWithUserImpl;
import org.veupathdb.service.eda.generated.model.MetricsUserProjectIdAnalysesGetStudyType;
import org.veupathdb.service.eda.generated.model.OnImportProvenanceProps;
import org.veupathdb.service.eda.generated.model.StudyCount;
import org.veupathdb.service.eda.generated.model.StudyCountImpl;
import org.veupathdb.service.eda.generated.model.UsersObjectsCount;
import org.veupathdb.service.eda.generated.model.UsersObjectsCountImpl;
import org.veupathdb.service.eda.user.Utils;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.gusdb.fgputil.functional.Functions.*;
import static org.gusdb.fgputil.functional.Functions.mapException;
import static org.veupathdb.service.eda.user.Utils.mapIfPresent;

/**
 * Performs all database operations for the user service
 */
public class UserDataFactory {

  private static final Logger LOG = LogManager.getLogger(UserDataFactory.class);

  private static final String SCHEMA_MACRO = "$SCHEMA$";
  private static final String TABLE_USERS = SCHEMA_MACRO + "users";
  private static final String TABLE_ANALYSIS = SCHEMA_MACRO + "analysis";
  private static final String TABLE_DERIVED_VARS = SCHEMA_MACRO + "derived_variables";

  // constants for analysis table columns
  private static final String COL_ANALYSIS_ID = "analysis_id"; // varchar(50) not null,
  private static final String COL_USER_ID = "user_id"; // integer not null,
  private static final String COL_STUDY_ID = "study_id"; // varchar(50) not null,
  private static final String COL_STUDY_VERSION = "study_version"; // varchar(50),
  private static final String COL_API_VERSION = "api_version"; // varchar(50),
  private static final String COL_DISPLAY_NAME = "display_name"; // varchar(50) not null,
  private static final String COL_DESCRIPTION = "description"; // varchar(4000),
  private static final String COL_CREATION_TIME = "creation_time"; // timestamp not null,
  private static final String COL_MODIFICATION_TIME = "modification_time"; // timestamp not null,
  private static final String COL_IS_PUBLIC = "is_public"; // integer not null,
  private static final String COL_NUM_FILTERS = "num_filters"; // integer not null,
  private static final String COL_NUM_COMPUTATIONS = "num_computations"; // integer not null,
  private static final String COL_NUM_VISUALIZATIONS = "num_visualizations"; // integer not null,
  private static final String COL_DESCRIPTOR = "analysis_descriptor"; // clob,
  private static final String COL_NOTES = "notes"; // clob,
  private static final String COL_PROVENANCE = "provenance"; // clob,


  private static final String[] SUMMARY_COLS = {
      COL_ANALYSIS_ID, // varchar(50) not null,
      COL_USER_ID, // integer not null,
      COL_STUDY_ID, // varchar(50) not null,
      COL_STUDY_VERSION, // varchar(50),
      COL_API_VERSION, // varchar(50),
      COL_DISPLAY_NAME, // varchar(50) not null,
      COL_DESCRIPTION, // varchar(4000),
      COL_CREATION_TIME, // timestamp not null,
      COL_MODIFICATION_TIME, // timestamp not null,
      COL_IS_PUBLIC, // integer not null,
      COL_NUM_FILTERS, // integer not null,
      COL_NUM_COMPUTATIONS, // integer not null,
      COL_NUM_VISUALIZATIONS, // integer not null,
      COL_PROVENANCE // clob,
  };

  private static final String[] DETAIL_COLS = ArrayUtil.concatenate(
      SUMMARY_COLS,
      new String[]{
          COL_DESCRIPTOR,
          COL_NOTES
      }
  );

  private static final Integer[] DETAIL_COL_TYPES = new Integer[] {
      Types.VARCHAR, // analysis_id
      Types.BIGINT, // user_id
      Types.VARCHAR, // study_id
      Types.VARCHAR, // study_version
      Types.VARCHAR, // api_version
      Types.VARCHAR, // display_name
      Types.VARCHAR, // description
      Types.TIMESTAMP, // creation_time
      Types.TIMESTAMP, // modification_time
      Resources.getUserPlatform().getBooleanType(), // is_public
      Types.INTEGER, // num_filters
      Types.INTEGER, // num_computations
      Types.INTEGER, // num_visualizations
      Types.CLOB, // provenance
      Types.CLOB, // descriptor
      Types.CLOB // notes
  };
  private static final String REPORT_MONTH_COL = "report_month";

  private final String _userSchema;
  private final String _metricsReportsSchema;

  public UserDataFactory(String projectId) {
    _userSchema = Resources.getUserDbSchema(projectId);
    _metricsReportsSchema = Resources.getMetricsReportSchema();
  }

  private String addSchema(String sqlConstant) {
    return sqlConstant.replace(SCHEMA_MACRO, _userSchema);
  }

  /**
   * Ensures all exceptions are logged and converted to runtime exceptions with a user-friendly message
   */
  private static final Function<Exception,RuntimeException> EXCEPTION_HANDLER = e -> {
    LOG.error(e.getMessage(), e);
    throw new RuntimeException("Unable to complete requested operation", e);
  };

  /***************************************************************************************
   *** Select Multiple Derived Variables
   **************************************************************************************/

  private static final String
    DV_COL_VARIABLE_ID   = "variable_id",
    DV_COL_USER_ID       = "user_id",
    DV_COL_DATASET_ID    = "dataset_id",
    DV_COL_ENTITY_ID     = "entity_id",
    DV_COL_DISPLAY_NAME  = "display_name",
    DV_COL_DESCRIPTION   = "description",
    DV_COL_PROVENANCE    = "provenance",
    DV_COL_FUNCTION_NAME = "function_name",
    DV_COL_CONFIG        = "config";

  private static DerivedVariableRow resultSetToDVRow(ResultSet rs) {
    return mapException(() -> new DerivedVariableRow(
      rs.getString(DV_COL_VARIABLE_ID),
      rs.getLong(DV_COL_USER_ID),
      rs.getString(DV_COL_DATASET_ID),
      rs.getString(DV_COL_ENTITY_ID),
      rs.getString(DV_COL_DISPLAY_NAME),
      rs.getString(DV_COL_DESCRIPTION),
      mapIfPresent(rs.getString(DV_COL_PROVENANCE), raw -> new DerivedVariableProvenance(new JSONObject(raw))),
      rs.getString(DV_COL_FUNCTION_NAME),
      with(rs.getString(DV_COL_CONFIG), c -> mapException(() -> Utils.JSON.readTree(c), EXCEPTION_HANDLER))
    ), EXCEPTION_HANDLER);
  }

  /**
   * Prefix SQL for bulk selecting derived variables by ID.
   *
   * <p>
   * Note the apostrophe at the end of the SQL prefix.
   * </p>
   */
  // language=Oracle
  private static final String BULK_SELECT_DERIVED_VARS_PREFIX =
    "SELECT variable_id, user_id, dataset_id, entity_id, display_name, "
      + "description, provenance, function_name, config FROM "
      + TABLE_DERIVED_VARS + " WHERE variable_id IN ('";

  /**
   * Suffix SQL for bulk selecting derived variables by ID.
   *
   * <p>
   * Note the apostrophe at the beginning of this SQL suffix.
   * </p>
   */
  private static final String BULK_SELECT_DERIVED_VARS_SUFFIX = "')";

  /**
   * Returns a list of {@link DerivedVariableRow} instances matching the list of
   * given input IDs.  If an input ID does not match a row in the database, no
   * row for that ID will be returned.
   *
   * <p>
   * <b>WARNING</b>: An SQL exception will be thrown if the given input list of
   * IDs contains more than {@code 1000} elements.
   * </p>
   *
   * @param ids List of derived variable IDs for the rows to look up.
   * <br>
   * THIS LIST MUST BE VALIDATED BEFOREHAND TO PREVENT SQL INJECTION!  Derived
   * variable IDs must be UUID values.
   *
   * @return A list of {@link DerivedVariableRow} instances that were found to
   * match one of the given input IDs.  This list will be at most the same size
   * as the input list of IDs, but may be smaller.
   */
  public List<DerivedVariableRow> getDerivedVariables(Collection<String> ids) {
    var sql = addSchema(ids.stream()
      .collect(Collectors.joining("','", BULK_SELECT_DERIVED_VARS_PREFIX, BULK_SELECT_DERIVED_VARS_SUFFIX)));

    LOG.debug("Bulk derived variable lookup for {} derived vars using the query {}", ids.size(), sql);

    return mapException(() -> new SQLRunner(Resources.getUserDataSource(), sql)
      .executeQuery(rs -> {
        var out = new ArrayList<DerivedVariableRow>(ids.size());

        while (rs.next())
          out.add(resultSetToDVRow(rs));

        return out;
      }), EXCEPTION_HANDLER);
  }

  /***************************************************************************************
   *** Patch Derived Variable
   **************************************************************************************/

  // language=Oracle
  private static final String PATCH_DERIVED_VAR_SQL =
    "UPDATE " + TABLE_DERIVED_VARS + " SET display_name = ?, description = ? WHERE variable_id = ?";

  public void patchDerivedVariable(String variableId, String displayName, String description) {
    mapException(() -> {
      LOG.debug("Patching derived variable # " + variableId);
      new SQLRunner(Resources.getUserDataSource(), addSchema(PATCH_DERIVED_VAR_SQL))
        .executeUpdate(
          new Object[]{ displayName, description, variableId },
          new Integer[]{ Types.VARCHAR, Types.CLOB, Types.VARCHAR }
        );
    }, EXCEPTION_HANDLER);
  }

  /***************************************************************************************
   *** Select Derived Variable
   **************************************************************************************/

  public Optional<DerivedVariableRow> getDerivedVariableById(String variableId) {
    return with(getDerivedVariables(List.of(variableId)), list -> list.isEmpty() ? Optional.empty() : Optional.of(list.get(0)));
  }

  /***************************************************************************************
   *** Select Derived Variables By User
   **************************************************************************************/

  // language=Oracle
  private static final String SELECT_DERIVED_VAR_BY_USER_SQL =
    "SELECT variable_id, user_id, dataset_id, entity_id, display_name, "
      + "description, provenance, function_name, config FROM "
      + TABLE_DERIVED_VARS + " WHERE user_id = ?";

  public List<DerivedVariableRow> getDerivedVariablesForUser(long userID) {
    return mapException(() -> {
      LOG.debug("Looking up derived variables for user " + userID);
      return new SQLRunner(Resources.getUserDataSource(), addSchema(SELECT_DERIVED_VAR_BY_USER_SQL))
        .executeQuery(
          new Object[]{ userID },
          new Integer[]{ Types.BIGINT },
          rs -> {
            var out = new ArrayList<DerivedVariableRow>();

            while (rs.next())
              out.add(resultSetToDVRow(rs));

            return out;
          }
        );
    }, EXCEPTION_HANDLER);
  }

  /***************************************************************************************
   *** Insert Derived Variable
   **************************************************************************************/

  // language=Oracle
  private static final String INSERT_DERIVED_VAR_SQL =
    "INSERT INTO " + TABLE_DERIVED_VARS + "(variable_id, user_id, dataset_id, "
      + "entity_id, display_name, description, provenance, function_name,"
      + "config) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private static final Integer[] INSERT_DERIVED_VAR_TYPES = {
    Types.VARCHAR,     // variable_id
    Types.BIGINT,      // user_id
    Types.VARCHAR,     // dataset_id
    Types.VARCHAR,     // entity_id
    Types.VARCHAR,     // display_name
    Types.CLOB,        // description
    Types.CLOB,        // provenance
    Types.VARCHAR,     // function_name
    Types.CLOB,        // config
  };

  /**
   * Converts the given {@link DerivedVariableRow} to an array of objects
   * suitable to be fed to the {@code INSERT_DERIVED_VAR_SQL} query.
   *
   * @param row Row to be converted.
   *
   * @return Object array containing the given row's fields in the order
   * expected by the {@code INSERT_DERIVED_VAR_SQL} query.
   */
  private static Object[] derivedVarToInsertRow(DerivedVariableRow row) {
    return new Object[] {
      row.getVariableID(),
      row.getUserID(),
      row.getDatasetID(),
      row.getEntityID(),
      row.getDisplayName(),
      row.getDescription(),
      mapIfPresent(row.getProvenance(), p -> p.toJSONObject().toString()),
      row.getFunctionName(),
      row.getConfig().toString(),
    };
  }

  public void addDerivedVariable(DerivedVariableRow derivedVariable) {
    LOG.debug("Insert derived variable # " + derivedVariable.getVariableID());

    mapException(() -> {
      new SQLRunner(Resources.getUserDataSource(), addSchema(INSERT_DERIVED_VAR_SQL))
        .executeUpdate(derivedVarToInsertRow(derivedVariable), INSERT_DERIVED_VAR_TYPES);
    }, EXCEPTION_HANDLER);
  }

  public void addDerivedVariables(Collection<DerivedVariableRow> rows) {
    LOG.debug("Bulk inserting {} derived variable rows", rows.size());

    mapException(() -> {
      new SQLRunner(Resources.getUserDataSource(), addSchema(INSERT_DERIVED_VAR_SQL))
        .executeStatementBatch(also(new BasicArgumentBatch(), bab -> {
          bab.setParameterTypes(INSERT_DERIVED_VAR_TYPES);
          rows.forEach(row -> bab.add(derivedVarToInsertRow(row)));
        }));
    }, EXCEPTION_HANDLER);
  }

  /***************************************************************************************
   *** Insert user
   **************************************************************************************/

  private static final String INSERT_USER_SQL =
      "insert into " + TABLE_USERS +
      " select %d as user_id, %d as is_guest, '{}' as preferences from dual" +
      " where not exists (select user_id from " + TABLE_USERS + " where user_id = %d)";

  public void addUserIfAbsent(User user) {
    mapException(() -> {
      // need to use format vs prepared statement for first two macros since they are in a select
      String sql = String.format(
          addSchema(INSERT_USER_SQL),
          user.getUserID(),
          Resources.getUserPlatform().convertBoolean(user.isGuest()),
          user.getUserID());
      LOG.debug("Trying to insert user with SQL: " + sql);
      int newRows = new SQLRunner(Resources.getUserDataSource(), sql, "insert-user").executeUpdate();
      LOG.debug(newRows == 0 ? "User with ID " + user.getUserID() + " already present." : "New user inserted.");
    }, EXCEPTION_HANDLER);
  }

  /***************************************************************************************
   *** Read preferences
   **************************************************************************************/

  private static final String READ_PREFS_SQL =
      "select preferences" +
      " from " + TABLE_USERS +
      " where user_id = ?";

  public String readPreferences(long userId) {
    return mapException(() ->
      new SQLRunner(
          Resources.getUserDataSource(),
          addSchema(READ_PREFS_SQL),
          "read-prefs"
      ).executeQuery(
          new Object[]{ userId },
          new Integer[]{ Types.BIGINT },
          rs -> rs.next()
            ? Resources.getUserPlatform().getClobData(rs, "preferences")
            : "{}"
      ), EXCEPTION_HANDLER);
  }

  /***************************************************************************************
   *** Write preferences
   **************************************************************************************/

  private static final String WRITE_PREFS_SQL =
      "update " + TABLE_USERS +
      " set preferences = ?" +
      " where user_id = ?";

  public void writePreferences(long userId, String prefsObject) {
    mapException(() ->
      new SQLRunner(
          Resources.getUserDataSource(),
          addSchema(WRITE_PREFS_SQL),
          "write-prefs"
      ).executeStatement(
          new Object[]{prefsObject, userId},
          new Integer[]{Types.CLOB, Types.BIGINT}
      ), EXCEPTION_HANDLER);
  }

  /***************************************************************************************
   *** Get user's analyses
   **************************************************************************************/

  private static final String GET_ANALYSES_BY_USER_SQL =
      "select " + String.join(", ", SUMMARY_COLS) +
      " from " + TABLE_ANALYSIS +
      " where " + COL_USER_ID + " = ?" +
      " order by " + COL_MODIFICATION_TIME + " desc";

  public List<AnalysisSummary> getAnalysisSummaries(long userId) {
    return mapException(() ->
      new SQLRunner(
          Resources.getUserDataSource(),
          addSchema(GET_ANALYSES_BY_USER_SQL),
          "analysis-summaries"
      ).executeQuery(
          new Object[]{ userId },
          new Integer[]{ Types.BIGINT },
          rs -> {
            List<AnalysisSummary> list = new ArrayList<>();
            while (rs.next()) {
              AnalysisSummary sum = new AnalysisSummaryImpl();
              populateSummaryFields(sum, rs);
              list.add(sum);
            }
            return list;
          }
      ), EXCEPTION_HANDLER);
  }

  /***************************************************************************************
   *** Get single analysis
   **************************************************************************************/

  private static final String GET_ANALYSIS_BY_ID_SQL =
      "select " + String.join(", ", DETAIL_COLS) +
      " from " + TABLE_ANALYSIS +
      " where " + COL_ANALYSIS_ID + " = ?";

  public AnalysisDetailWithUser getAnalysisById(String analysisId) {
    // must declare the type here; Java not smart enough yet if we substitute it below
    SupplierWithException<Optional<AnalysisDetailWithUser>> getter = () ->
      new SQLRunner(
        Resources.getUserDataSource(),
        addSchema(GET_ANALYSIS_BY_ID_SQL),
        "analysis-detail"
      ).executeQuery(
        new Object[]{ analysisId },
        new Integer[]{ Types.VARCHAR },
        rs -> {
          if (!rs.next()) {
            return Optional.empty();
          }
          AnalysisDetailWithUser analysis = new AnalysisDetailWithUser(rs);
          if (rs.next()) {
            throw new IllegalStateException("More than one analysis found with ID: " + analysisId);
          }
          return Optional.of(analysis);
        }
      );
    return mapException(getter, EXCEPTION_HANDLER).orElseThrow(NotFoundException::new);
  }

  /***************************************************************************************
   *** Insert analysis
   **************************************************************************************/

  private static final String INSERT_ANALYSIS_SQL =
      "insert into " + TABLE_ANALYSIS + " ( " +
        String.join(", ", DETAIL_COLS) +
      " ) values ( " +
        Arrays.stream(DETAIL_COLS).map(c -> "?").collect(Collectors.joining(", ")) +
      " ) ";

  public void insertAnalysis(AnalysisDetailWithUser analysis) {
    mapException(() -> new SQLRunner(
        Resources.getUserDataSource(),
        addSchema(INSERT_ANALYSIS_SQL),
        "insert-analysis"
    ).executeStatement(
        getAnalysisInsertValues(analysis),
        DETAIL_COL_TYPES
    ), EXCEPTION_HANDLER);
  }

  /***************************************************************************************
   *** Update analysis
   **************************************************************************************/

  private static final String UPDATE_ANALYSIS_SQL =
      "update " + TABLE_ANALYSIS + " set " +
      Arrays.stream(DETAIL_COLS).map(c -> c + " = ?").collect(Collectors.joining(", ")) +
      " where " + COL_ANALYSIS_ID + " = ?";

  public void updateAnalysis(AnalysisDetailWithUser analysis) {
    mapException(() -> {
      int rowsUpdated = new SQLRunner(
          Resources.getUserDataSource(),
          addSchema(UPDATE_ANALYSIS_SQL),
          "update-analysis"
      ).executeUpdate(
          ArrayUtil.concatenate(
              getAnalysisInsertValues(analysis),
              new Object[]{analysis.getAnalysisId()} // extra ? for analysis_id in where statement
          ),
          ArrayUtil.concatenate(
              DETAIL_COL_TYPES,
              new Integer[]{Types.VARCHAR} // extra ? for analysis_id in where statement
          )
      );
      if (rowsUpdated != 1) {
        throw new IllegalStateException("Updated " + rowsUpdated +
            " rows (should be 1) in DB when updated analysis with ID " + analysis.getAnalysisId());
      }
    }, EXCEPTION_HANDLER);
  }

  /***************************************************************************************
   *** Delete a set of analyses
   **************************************************************************************/

  private static final String IDS_MACRO_LIST_MACRO = "$MACRO_LIST$";
  private static final String DELETE_ANALYSES_SQL =
      "delete from " + TABLE_ANALYSIS +
      " where " + COL_ANALYSIS_ID + " IN  ( " + IDS_MACRO_LIST_MACRO + " )";

  public void deleteAnalyses(String... idsToDelete) {

    // check for valid number of IDs
    if (idsToDelete.length == 0) return;
    if (idsToDelete.length > 200) throw new BadRequestException("Too many IDS (max = 200)");

    // construct prepared statement SQL with the right number of macros for all the IDs
    String[] stringArr = new String[idsToDelete.length];
    Arrays.fill(stringArr, "?");
    String sql = DELETE_ANALYSES_SQL.replace(IDS_MACRO_LIST_MACRO, String.join(", ", stringArr));

    mapException(() ->
      new SQLRunner(
        Resources.getUserDataSource(),
        addSchema(sql),
        "delete-analyses"
      ).executeStatement(
        idsToDelete
      ), EXCEPTION_HANDLER);
  }

  /***************************************************************************************
   *** Get public analyses
   **************************************************************************************/

  private static final String GET_PUBLIC_ANALYSES_SQL =
      "select " + String.join(", ", SUMMARY_COLS) +
      " from " + TABLE_ANALYSIS +
      " where " + COL_IS_PUBLIC + " = " + Resources.getUserPlatform().convertBoolean(true) +
      " order by " + COL_MODIFICATION_TIME + " desc";

  public List<AnalysisSummaryWithUser> getPublicAnalyses() {
    return mapException(() ->
        new SQLRunner(
          Resources.getUserDataSource(),
          addSchema(GET_PUBLIC_ANALYSES_SQL),
          "public-analysis-summaries"
        ).executeQuery(
          rs -> {
            List<AnalysisSummaryWithUser> list = new ArrayList<>();
            while (rs.next()) {
              AnalysisSummaryWithUser sum = new AnalysisSummaryWithUserImpl();
              sum.setUserId(rs.getLong(COL_USER_ID));
              populateSummaryFields(sum, rs);
              list.add(sum);
            }
            return list;
          }
        ), EXCEPTION_HANDLER);
  }

  /***************************************************************************************
   *** Transfer guest analyses to logged in user
   **************************************************************************************/

  private static final String TRANSFER_GUEST_ANALYSES_SQL =
      "update " + TABLE_ANALYSIS +
      " set user_id = ?" +
      " where analysis_id in (" +
      "   select analysis_id" +
      "   from " + TABLE_ANALYSIS + " a, " + TABLE_USERS + " u" +
      "   where a.user_id = u.user_id" +
      "   and u.is_guest = " + Resources.getUserPlatform().convertBoolean(true) +
      "   and u.user_id = ?" +
      " )";

  public void transferGuestAnalysesOwnership(long fromGuestUserId, long toRegisteredUserId) {
    mapException(() ->
      new SQLRunner(
        Resources.getUserDataSource(),
        addSchema(TRANSFER_GUEST_ANALYSES_SQL),
        "transfer-analyses"
      ).executeStatement(
        new Object[]{ toRegisteredUserId, fromGuestUserId }
      ), EXCEPTION_HANDLER);
  }

  /***************************************************************************************
   *** Read analysis metrics
   **************************************************************************************/

  public enum Imported {
    YES, NO
  }

  // this enum supports different ways to define a date range.  at this point only one is in use.  we can simplify at some point.
  public enum DateColumn {
    CREATION("creation_time", ""),
    CREATION_OR_MODIFICATION("modification_time", ""),
    MODIFICATION("modification_time", " AND modification_time != creation_time ");
    private final String label;
    private final String andClause;
    DateColumn(String label, String andClause) {
      this.label = label;
      this.andClause = andClause;
    }
  }

  public enum IsGuest {
    YES(1), NO(0);
    private final int flag;
    IsGuest(int flag) { this.flag = flag;}
  }

  public List<String> getIgnoreInMetricsUserIds() {
    String sql = """
      select user_id from useraccounts.account_properties
      where key = 'ignore_in_metrics' and value = 'true'
    """;
    return mapException(() ->
        new SQLRunner(
            Resources.getAccountsDataSource(),
            sql,
            "get-ignore-in-metrics-user-ids"
        ).executeQuery(
            rs -> {
              List<String> userIds = new ArrayList<>();
              while (rs.next()) userIds.add(Integer.toString(rs.getInt("USER_ID")));
              return userIds;
            }
        ), EXCEPTION_HANDLER);
  }

  static private String getStudyTypeSql(MetricsUserProjectIdAnalysesGetStudyType studyType) {
    return switch (studyType) {
      case ALL -> "LIKE '%'";
      case USER -> "LIKE 'EDAUD_%'";
      case CURATED -> "NOT LIKE 'EDAUD_%'";
    };
  }

  public List<StudyCount> readAnalysisCountsByStudy(MetricsUserProjectIdAnalysesGetStudyType studyType, LocalDate startDate, LocalDate endDate, DateColumn dateColumn, String ignoreUserIds, Imported imported) {
    String sqlTemplate = """
        select count(analysis_id) as cnt, study_id
        from %sanalysis a, %susers u
        where %s > ? and %s <= ? %s
        and a.user_id = u.user_id
        and study_id %s
        and a.user_id NOT IN (%s)
        %s
        group by study_id
        order by cnt desc
                """;
    String importClause = imported == Imported.YES ? "and provenance is not null" + System.lineSeparator() : "";
    String sql = String.format(sqlTemplate, _userSchema, _userSchema, dateColumn.label, dateColumn.label, dateColumn.andClause,
            getStudyTypeSql(studyType), ignoreUserIds, importClause);

    return mapException(() ->
        new SQLRunner(
            Resources.getUserDataSource(),
            sql,
            "read-analysis-counts"
        ).executeQuery(
            new Object[]{ java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate) },
            new Integer[]{ Types.DATE, Types.DATE },
            rs -> {
              List<StudyCount> countPerStudy = new ArrayList<>();
              while (rs.next()) {
                int count = rs.getInt("CNT");
                String studyId = rs.getString("STUDY_ID");
                StudyCount sc = new StudyCountImpl();
                sc.setCount(count);
                sc.setStudyId(studyId);
                countPerStudy.add(sc);
              }
              return countPerStudy;
            }
        ), EXCEPTION_HANDLER);
  }

  // collect a histogram of counts of number of users with a number of some object (eg analyses or filters) from the analysis table.
  // the objects are aggregated by the aggregateObjectsSql.  EG:  "count(analysis_id)" or "sum(num_filters)"
  public List<UsersObjectsCount> readObjectCountsByUserCounts(MetricsUserProjectIdAnalysesGetStudyType studyType, String aggregateObjectSql, LocalDate startDate, LocalDate endDate, DateColumn dateColumn, String ignoreIdsString, IsGuest isGuest) {
    String sqlTemplate = """
        select count(user_id) as user_cnt, objects
        from (
          select %s as objects, a.user_id
          from %sanalysis a, %susers u
          where %s > ? and %s <= ? %s
          and a.user_id = u.user_id
          and study_id %s
          and a.user_id NOT IN (%s)
          and is_guest = ?
          group by a.user_id
        )
        group by objects
        order by objects desc
        """;

    String sql = String.format(sqlTemplate, aggregateObjectSql, _userSchema, _userSchema,
            dateColumn.label, dateColumn.label, dateColumn.andClause, getStudyTypeSql(studyType), ignoreIdsString);

    return mapException(() ->
        new SQLRunner(
            Resources.getUserDataSource(),
            sql,
            "read-user-object-counts"
        ).executeQuery(
            new Object[]{ java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate), isGuest.flag },
            new Integer[]{ Types.DATE, Types.DATE, Types.INTEGER },
            rs -> {
              List<UsersObjectsCount> counts = new ArrayList<>();
              while (rs.next()) {
                int userCount = rs.getInt("USER_CNT");
                int objectsCount = rs.getInt("OBJECTS");
                UsersObjectsCount uac = new UsersObjectsCountImpl();
                uac.setUsersCount(userCount);
                uac.setObjectsCount(objectsCount);
                counts.add(uac);
              }
              return counts;
            }
        ), EXCEPTION_HANDLER);
  }

  public void streamPerStudyAnalysisMetrics(int year, int month, TabularDataWriter tabularFormatter, String reportMonth) {
    String sql = """
        SELECT 
          m.dataset_id, 
          m.analysis_count,  
          m.shares_count
        FROM %sanalysismetricsperstudy m
        JOIN (
          SELECT MAX(report_id) report_id, report_month, report_year, MAX(report_time) FROM %sreports
          GROUP BY report_month, report_year
          HAVING report_month = ? AND report_year = ?
        ) r 
        ON r.report_id = m.report_id
    """.formatted(_metricsReportsSchema, _metricsReportsSchema);
    try {
      tabularFormatter.write("dataset_id", "analysis_count", "shares_count", REPORT_MONTH_COL);
      tabularFormatter.nextRecord();
    } catch (IOException e) {
      throw new RuntimeException("Unable to write headers.", e);
    }
    mapException(() ->
        new SQLRunner(
            Resources.getUserDataSource(),
            sql,
            "read-analysis-study"
        ).executeQuery(
            new Object[]{ month, year },
            rs -> {
              while (rs.next()) {
                try {
                  tabularFormatter.write(rs.getString("dataset_id"));
                  tabularFormatter.write(Integer.toString(rs.getInt("analysis_count")));
                  tabularFormatter.write(Integer.toString(rs.getInt("shares_count")));
                  tabularFormatter.write(reportMonth);
                  tabularFormatter.nextRecord();
                } catch (IOException e) {
                  throw new RuntimeException("Error while attempting to write to output stream.", e);
                }
              }
              return null;
            }
        ), EXCEPTION_HANDLER);
  }

  public void streamAggregateUserStats(int year, int month, TabularDataWriter tabularFormatter, String reportMonth) {
    final String categoryCol = "user_category";
    final String numUserCol = "num_users";
    final String numFiltersCol = "num_filters";
    final String numAnalysesCol = "num_analyses";
    final String numVizCol = "num_visualizations";
    String sql = """
        SELECT 
          s.%s, 
          s.%s,  
          s.%s,
          s.%s,
          s.%s
        FROM %saggregateuserstats s
        JOIN (
          SELECT MAX(report_id) report_id, report_month, report_year, MAX(report_time) FROM %sreports
          GROUP BY report_month, report_year
          HAVING report_month = ? AND report_year = ?
        ) r 
        ON r.report_id = s.report_id
        """.formatted(categoryCol, numUserCol, numFiltersCol, numAnalysesCol, numVizCol,
        _metricsReportsSchema, _metricsReportsSchema);
    try {
      tabularFormatter.write(categoryCol, numUserCol, numFiltersCol, numAnalysesCol, numVizCol, REPORT_MONTH_COL);
      tabularFormatter.nextRecord();
    } catch (IOException e) {
      throw new RuntimeException("Unable to write headers.", e);
    }

    mapException(() ->
        new SQLRunner(
            Resources.getUserDataSource(),
            sql,
            "read-analysis-totals"
        ).executeQuery(
            new Integer[]{ month, year },
            rs -> {
              while (rs.next()) {
                try {
                  tabularFormatter.write(rs.getString(categoryCol));
                  tabularFormatter.write(Integer.toString(rs.getInt(numUserCol)));
                  tabularFormatter.write(Integer.toString(rs.getInt(numFiltersCol)));
                  tabularFormatter.write(Integer.toString(rs.getInt(numAnalysesCol)));
                  tabularFormatter.write(Integer.toString(rs.getInt(numVizCol)));
                  tabularFormatter.write(reportMonth);
                  tabularFormatter.nextRecord();
                } catch (IOException e) {
                  throw new RuntimeException("Error while attempting to write to output stream.", e);
                }
              }
              return null;
            }
        ), EXCEPTION_HANDLER);
  }

  public void streamDownloadReport(int year, int month, TabularDataWriter recordFormatter, String reportMonth) {
    final String studyIdCol = "study_id";
    final String numUsersFullDownloadCol = "num_users_full_download";
    final String numUsersSubsetDownloadCol = "num_users_subset_download";
    String sql = """
        SELECT 
          d.%s, 
          d.%s,  
          d.%s
        FROM %sdownloadsperstudy d
        JOIN (
          SELECT MAX(report_id) report_id, report_month, report_year, MAX(report_time) FROM %sreports
          GROUP BY report_month, report_year
          HAVING report_month = ? AND report_year = ?
        ) r 
        ON r.report_id = d.report_id
    """.formatted(studyIdCol, numUsersFullDownloadCol, numUsersSubsetDownloadCol, _metricsReportsSchema, _metricsReportsSchema);

    try {
      recordFormatter.write(studyIdCol, numUsersFullDownloadCol, numUsersSubsetDownloadCol, REPORT_MONTH_COL);
      recordFormatter.nextRecord();
    } catch (IOException e) {
      throw new RuntimeException("Unable to write headers.", e);
    }

    mapException(() ->
        new SQLRunner(
            Resources.getUserDataSource(),
            sql,
            "read-download-report"
        ).executeQuery(
            new Integer[]{ month, year },
            rs -> {
              while (rs.next()) {
                try {
                  recordFormatter.write(rs.getString("study_id"));
                  recordFormatter.write(Integer.toString(rs.getInt("num_users_full_download")));
                  recordFormatter.write(Integer.toString(rs.getInt("num_users_subset_download")));
                  recordFormatter.write(reportMonth);
                  recordFormatter.nextRecord();
                } catch (IOException e) {
                  throw new RuntimeException("Error while attempting to write to output stream.", e);
                }
              }
              return null;
            }
        ), EXCEPTION_HANDLER);
  }

  public void streamAnalysisHistogram(int year, int month, TabularDataWriter tabularFormatter, String reportMonth) {
    String sql = """
        SELECT 
          h.count_bucket, 
          h.registered_users_analyses,  
          h.guests_analyses,
          h.registered_users_filters,
          h.guests_filters,
          h.registered_users_visualizations,
          h.guest_users_visualizations
        FROM %sanalysishistogram h
        JOIN (
          SELECT MAX(report_id) report_id, report_month, report_year, MAX(report_time) FROM %sreports
          GROUP BY report_month, report_year
          HAVING report_month = ? AND report_year = ?
        ) r 
        ON r.report_id = h.report_id
    """.formatted(_metricsReportsSchema, _metricsReportsSchema);
    try {
      tabularFormatter.write("count_bucket", "registered_users_analyses", "guests_analyses",
          "guests_filters", "registered_users_visualizations", "guest_user_visualizations", REPORT_MONTH_COL);
      tabularFormatter.nextRecord();
    } catch (Exception e) {
      throw new RuntimeException("Failed to write header rows", e);
    }
    mapException(() ->
        new SQLRunner(
            Resources.getUserDataSource(),
            sql,
            "read-analysis-histogram"
        ).executeQuery(
            new Integer[]{ month, year },
            rs -> {
              while (rs.next()) {
                try {
                  tabularFormatter.write(rs.getString("count_bucket"));
                  tabularFormatter.write(Integer.toString(rs.getInt("registered_users_analyses")));
                  tabularFormatter.write(Integer.toString(rs.getInt("guests_analyses")));
                  tabularFormatter.write(Integer.toString(rs.getInt("registered_users_filters")));
                  tabularFormatter.write(Integer.toString(rs.getInt("guests_filters")));
                  tabularFormatter.write(Integer.toString(rs.getInt("registered_users_visualizations")));
                  tabularFormatter.write(Integer.toString(rs.getInt("guest_users_visualizations")));
                  tabularFormatter.write(reportMonth);
                  tabularFormatter.nextRecord();
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              }
              return null;
            }
        ), EXCEPTION_HANDLER);
  }

  /***************************************************************************************
   *** Analysis object population methods
   **************************************************************************************/

  private static void populateSummaryFields(AnalysisSummary analysis, ResultSet rs) throws SQLException {
    analysis.setAnalysisId(rs.getString(COL_ANALYSIS_ID)); // varchar(50) not null,
    analysis.setStudyId(rs.getString(COL_STUDY_ID)); // varchar(50) not null,
    analysis.setStudyVersion(getStringOrEmpty(rs, COL_STUDY_VERSION)); // varchar(50),
    analysis.setApiVersion(getStringOrEmpty(rs, COL_API_VERSION)); // varchar(50),
    analysis.setDisplayName(rs.getString(COL_DISPLAY_NAME)); // varchar(50) not null,
    analysis.setDescription(getStringOrEmpty(rs, COL_DESCRIPTION)); // varchar(4000),
    analysis.setCreationTime(Utils.formatTimestamp(rs.getTimestamp(COL_CREATION_TIME))); // timestamp not null,
    analysis.setModificationTime(Utils.formatTimestamp(rs.getTimestamp(COL_MODIFICATION_TIME))); // timestamp not null,
    analysis.setIsPublic(Resources.getUserPlatform().getBooleanValue(rs, COL_IS_PUBLIC, false)); // integer not null,
    analysis.setNumFilters(rs.getInt(COL_NUM_FILTERS)); // integer not null,
    analysis.setNumComputations(rs.getInt(COL_NUM_COMPUTATIONS)); // integer not null,
    analysis.setNumVisualizations(rs.getInt(COL_NUM_VISUALIZATIONS)); // integer not null,
    analysis.setProvenance(createProvenance(Resources.getUserPlatform().getClobData(rs, COL_PROVENANCE))); // clob
  }

  private static AnalysisProvenance createProvenance(String onImportPropsString) {
    if (onImportPropsString == null) return null;
    AnalysisProvenance provenance = new AnalysisProvenanceImpl();
    provenance.setOnImport(Utils.parseObject(onImportPropsString, OnImportProvenanceProps.class));
    // current props will be assigned later
    return provenance;
  }

  private static String getStringOrEmpty(ResultSet rs, String colName) throws SQLException {
    return Optional.ofNullable(rs.getString(colName)).orElse("");
  }

  static void populateDetailFields(AnalysisDetailWithUser analysis, ResultSet rs) throws SQLException {
    analysis.setUserId(rs.getLong(COL_USER_ID));
    UserDataFactory.populateSummaryFields(analysis, rs);
    analysis.setDescriptor(Utils.parseObject(Resources.getUserPlatform().getClobData(rs, COL_DESCRIPTOR), AnalysisDescriptor.class)); // clob
    analysis.setNotes(Resources.getUserPlatform().getClobData(rs, COL_NOTES)); // clob
  }

  /***************************************************************************************
   *** Types and vals for analysis insert/update prepared statement vars
   **************************************************************************************/

  private static Object[] getAnalysisInsertValues(AnalysisDetailWithUser analysis) {
    return new Object[]{
        analysis.getAnalysisId(),
        analysis.getUserId(),
        analysis.getStudyId(),
        analysis.getStudyVersion(),
        analysis.getApiVersion(),
        analysis.getDisplayName(),
        analysis.getDescription(),
        Utils.parseDate(analysis.getCreationTime()),
        Utils.parseDate(analysis.getModificationTime()),
        Resources.getUserPlatform().convertBoolean(analysis.getIsPublic()),
        analysis.getNumFilters(),
        analysis.getNumComputations(),
        analysis.getNumVisualizations(),
        analysis.getProvenance() == null ? null : Utils.formatObject(analysis.getProvenance().getOnImport()),
        Utils.formatObject(analysis.getDescriptor()),
        analysis.getNotes()
    };
  }
}
