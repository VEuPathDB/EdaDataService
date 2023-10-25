package org.veupathdb.service.eda;

import jakarta.ws.rs.NotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.client.ClientUtil;
import org.gusdb.fgputil.db.platform.DBPlatform;
import org.gusdb.fgputil.db.platform.Oracle;
import org.gusdb.fgputil.runtime.Environment;
import org.gusdb.fgputil.runtime.ProjectSpecificProperties;
import org.veupathdb.lib.container.jaxrs.config.Options;
import org.veupathdb.lib.container.jaxrs.server.ContainerResources;
import org.veupathdb.lib.container.jaxrs.utils.db.DbManager;
import org.veupathdb.service.eda.access.controller.ApproveEligibleStudiesController;
import org.veupathdb.service.eda.access.controller.EndUserController;
import org.veupathdb.service.eda.access.controller.HistoryController;
import org.veupathdb.service.eda.access.controller.PermissionController;
import org.veupathdb.service.eda.access.controller.ProviderController;
import org.veupathdb.service.eda.access.controller.StaffController;
import org.veupathdb.service.eda.access.repo.ApprovalStatusRepo;
import org.veupathdb.service.eda.access.repo.RestrictionLevelRepo;
import org.veupathdb.service.eda.compute.service.JobsController;
import org.veupathdb.service.eda.download.DownloadService;
import org.veupathdb.service.eda.data.AppsService;
import org.veupathdb.service.eda.ss.model.reducer.BinaryValuesStreamer;
import org.veupathdb.service.eda.subset.service.ClearMetadataCacheService;
import org.veupathdb.service.eda.data.FilterAwareMetadataService;
import org.veupathdb.service.eda.merge.controller.MergingServiceExternal;
import org.veupathdb.service.eda.merge.controller.MergingServiceInternal;
import org.veupathdb.service.eda.subset.service.MetadataCache;
import org.veupathdb.service.eda.subset.service.StudiesService;
import org.veupathdb.service.eda.subset.service.SubsettingServiceInternal;
import org.veupathdb.service.eda.ss.model.variable.binary.BinaryFilesManager;
import org.veupathdb.service.eda.ss.model.variable.binary.SimpleStudyFinder;
import org.veupathdb.service.eda.ss.test.StubDb;
import org.veupathdb.service.eda.subset.SubsetEnvironment;
import org.veupathdb.service.eda.user.service.ImportAnalysisService;
import org.veupathdb.service.eda.user.service.MetricsService;
import org.veupathdb.service.eda.user.service.PublicDataService;
import org.veupathdb.service.eda.user.service.UserService;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.gusdb.fgputil.runtime.Environment.getRequiredVar;
import static org.gusdb.fgputil.runtime.ProjectSpecificProperties.PropertySpec.required;

/**
 * Service Resource Registration.
 *
 * This is where all the individual service specific resources and middleware
 * should be registered.
 */
public class Resources extends ContainerResources {
  private static final Logger LOG = LogManager.getLogger(Resources.class);


  // Subsetting Config
  private static final CountDownLatch DB_INIT_SIGNAL = new CountDownLatch(1);
  private static final SubsetEnvironment SUBSET_ENV = new SubsetEnvironment();
  private static final BinaryFilesManager BINARY_FILES_MANAGER = new BinaryFilesManager(
      new SimpleStudyFinder(Resources.getBinaryFilesDirectory().toString()));
  private static final MetadataCache METADATA_CACHE = new MetadataCache(BINARY_FILES_MANAGER, DB_INIT_SIGNAL);
  private static final ExecutorService FILE_READ_THREAD_POOL = Executors.newCachedThreadPool();
  private static final ExecutorService DESERIALIZER_THREAD_POOL = Executors.newFixedThreadPool(16);

  // use in-memory test DB unless "real" application DB is configured
  private static boolean USE_IN_MEMORY_TEST_DATABASE = true;

  // Download Files Config
  public static final Path DOWNLOAD_FILES_MOUNT_PATH = getReadableDir(Paths.get(Environment.getRequiredVar("DOWNLOAD_FILES_MOUNT_PATH")));
  private static final String USER_SCHEMA_PROP = "USER_SCHEMA";

  // Project-specific config
  private static Map<String,String> SCHEMA_MAP;
  private static final String RAW_FILES_DIR_PROP = "RAW_FILES_DIR";
  private static Map<String,String> PROJECT_DIR_MAP;

  // Service URLs -- these can be removed once inter-component communication is done in-memory.
  public static final String SUBSETTING_SERVICE_URL = getRequiredVar("SUBSETTING_SERVICE_URL");
  public static final String MERGING_SERVICE_URL = getRequiredVar("MERGING_SERVICE_URL");
  public static final String COMPUTE_SERVICE_URL = getRequiredVar("COMPUTE_SERVICE_URL");
  public static final String DATASET_ACCESS_SERVICE_URL = getRequiredVar("DATASET_ACCESS_SERVICE_URL");
  public static final String RSERVE_URL = getRequiredVar("RSERVE_URL");

  public Resources(Options opts) {
    super(opts);

    // initialize auth and required DBs
    DbManager.initUserDatabase(opts);
    DbManager.initAccountDatabase(opts);
    enableAuth();

    // Project-specific user schemas.
    SCHEMA_MAP = new ProjectSpecificProperties<>(
        new ProjectSpecificProperties.PropertySpec[] { required(USER_SCHEMA_PROP) },
        map -> {
          // add trailing '.' to schema names for convenience later
          String rawSchemaName = map.get(USER_SCHEMA_PROP);
          return rawSchemaName + (rawSchemaName.endsWith(".") ? "" : ".");
        }
    ).toMap();

    // Project-specific download directories. Study download files have a project-aware directory structure.
    PROJECT_DIR_MAP = new ProjectSpecificProperties<>(
        new ProjectSpecificProperties.PropertySpec[] { required(RAW_FILES_DIR_PROP) },
        map -> map.get(RAW_FILES_DIR_PROP)
    ).toMap();

    if (opts.getAppDbOpts().name().isPresent() || opts.getAppDbOpts().tnsName().isPresent()) {
      // application database configured; use it
      USE_IN_MEMORY_TEST_DATABASE = false;
    } else {
      LOG.warn("No application database configured! Using in-memory database. This should only appear in test environments.");
    }

    // load cached permissions data.
    try {
      ApprovalStatusRepo.Select.populateApprovalStatusCache();
      RestrictionLevelRepo.Select.populateRestrictionLevelCache();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }

    if (!USE_IN_MEMORY_TEST_DATABASE) {
      DbManager.initApplicationDatabase(opts);
      LOG.info("Using application DB connection URL: " +
          DbManager.getInstance().getApplicationDatabase().getConfig().getConnectionUrl());

      // Signal to dependencies that database is available.
      DB_INIT_SIGNAL.countDown();
    }
  }

  public static MetadataCache getMetadataCache() {
    return METADATA_CACHE;
  }

  public static boolean isFileBasedSubsettingEnabled() {
    return SUBSET_ENV.isFileBasedSubsettingEnabled();
  }

  public static BinaryFilesManager getBinaryFilesManager() {
    return BINARY_FILES_MANAGER;
  }

  public static DataSource getApplicationDataSource() {
    return USE_IN_MEMORY_TEST_DATABASE
      ? StubDb.getDataSource()
      : DbManager.applicationDatabase().getDataSource();
  }

  public static String getAppDbSchema() {
    return USE_IN_MEMORY_TEST_DATABASE ? "" : SUBSET_ENV.getAppDbSchema();
  }

  public static String getUserStudySchema() {
    return USE_IN_MEMORY_TEST_DATABASE ? "" : SUBSET_ENV.getUserStudySchema();
  }

  public static Path getBinaryFilesDirectory() {
    return Path.of(SUBSET_ENV.getBinaryFilesMount(), SUBSET_ENV.getBinaryFilesDirectory().replace("%DB_BUILD%", SUBSET_ENV.getDbBuild()));
  }

  public static ExecutorService getFileChannelThreadPool() {
    return FILE_READ_THREAD_POOL;
  }

  public static ExecutorService getDeserializerThreadPool() {
    return DESERIALIZER_THREAD_POOL;
  }

  public static String getDatasetAccessServiceUrl() {
    return SUBSET_ENV.getDatasetAccessServiceUrl();
  }

  public static DataSource getUserDataSource() {
    return DbManager.userDatabase().getDataSource();
  }

  public static DataSource getAccountsDataSource() { return DbManager.accountDatabase().getDataSource(); }

  public static String getUserDbSchema(String projectId) {
    if (!SCHEMA_MAP.containsKey(projectId)) {
      throw new NotFoundException("Invalid project ID: " + projectId);
    }
    return SCHEMA_MAP.get(projectId);
  }

  public static DBPlatform getUserPlatform() {
    return new Oracle();
  }

  public static String getMetricsReportSchema() {
    return Environment.getOptionalVar("USAGE_METRICS_SCHEMA", "usagemetrics.");
  }

  public static Path getDatasetsParentDir(String projectId) {
    if (!PROJECT_DIR_MAP.containsKey(projectId)) {
      throw new NotFoundException("Invalid project ID: " + projectId);
    }
    return getReadableDir(DOWNLOAD_FILES_MOUNT_PATH.resolve(PROJECT_DIR_MAP.get(projectId)));
  }

  private static Path getReadableDir(Path dirPath) {
    if (Files.isDirectory(dirPath) && Files.isReadable(dirPath)) {
      return dirPath;
    }
    throw new RuntimeException("Configured data dir '" + dirPath + "' is not a readable directory.");
  }

  public static BinaryValuesStreamer getBinaryValuesStreamer() {
    return new BinaryValuesStreamer(BINARY_FILES_MANAGER, FILE_READ_THREAD_POOL, DESERIALIZER_THREAD_POOL);
  }


  /**
   * Returns an array of JaxRS endpoints, providers, and contexts.
   *
   * Entries in the array can be either classes or instances.
   */
  @Override
  protected Object[] resources() {
    return new Object[]{
        // Subsetting
        StudiesService.class,
        SubsettingServiceInternal.class,
        ClearMetadataCacheService.class,
        // Visualization
        AppsService.class,
        FilterAwareMetadataService.class,
        // Merging
        MergingServiceExternal.class,
        MergingServiceInternal.class,
        // Compute
        JobsController.class,
        // Access
        ApproveEligibleStudiesController.class,
        ProviderController.class,
        StaffController.class,
        EndUserController.class,
        PermissionController.class,
        HistoryController.class,
        // User
        UserService.class,
        PublicDataService.class,
        ImportAnalysisService.class,
        MetricsService.class,
        // Download
        DownloadService.class
    };
  }
}
