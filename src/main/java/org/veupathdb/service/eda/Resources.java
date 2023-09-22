package org.veupathdb.service.eda;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.client.ClientUtil;
import org.veupathdb.lib.container.jaxrs.config.Options;
import org.veupathdb.lib.container.jaxrs.server.ContainerResources;
import org.veupathdb.lib.container.jaxrs.utils.db.DbManager;
import org.veupathdb.service.eda.service.AppsService;
import org.veupathdb.service.eda.service.ClearMetadataCacheService;
import org.veupathdb.service.eda.service.FilterAwareMetadataService;
import org.veupathdb.service.eda.service.MergingServiceExternal;
import org.veupathdb.service.eda.service.MergingServiceInternal;
import org.veupathdb.service.eda.service.MetadataCache;
import org.veupathdb.service.eda.service.StudiesService;
import org.veupathdb.service.eda.service.SubsettingServiceInternal;
import org.veupathdb.service.eda.ss.model.variable.binary.BinaryFilesManager;
import org.veupathdb.service.eda.ss.model.variable.binary.SimpleStudyFinder;
import org.veupathdb.service.eda.ss.test.StubDb;
import org.veupathdb.service.eda.subsetting.EnvironmentVars;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.gusdb.fgputil.runtime.Environment.getOptionalVar;
import static org.gusdb.fgputil.runtime.Environment.getRequiredVar;

/**
 * Service Resource Registration.
 *
 * This is where all the individual service specific resources and middleware
 * should be registered.
 */
public class Resources extends ContainerResources {
  private static final Logger LOG = LogManager.getLogger(Resources.class);

  private static final EnvironmentVars ENV = new EnvironmentVars();

  private static final BinaryFilesManager BINARY_FILES_MANAGER = new BinaryFilesManager(
      new SimpleStudyFinder(Resources.getBinaryFilesDirectory().toString()));

  private static final MetadataCache METADATA_CACHE = new MetadataCache(BINARY_FILES_MANAGER);

  private static final ExecutorService FILE_READ_THREAD_POOL = Executors.newCachedThreadPool();

  private static final ExecutorService DESERIALIZER_THREAD_POOL = Executors.newFixedThreadPool(16);

  private static final boolean DEVELOPMENT_MODE =
      Boolean.parseBoolean(getOptionalVar("DEVELOPMENT_MODE", "false"));

  public static final String SUBSETTING_SERVICE_URL = getRequiredVar("SUBSETTING_SERVICE_URL");
  public static final String MERGING_SERVICE_URL = getRequiredVar("MERGING_SERVICE_URL");
  public static final String COMPUTE_SERVICE_URL = getRequiredVar("COMPUTE_SERVICE_URL");
  public static final String DATASET_ACCESS_SERVICE_URL = getRequiredVar("DATASET_ACCESS_SERVICE_URL");
  public static final String RSERVE_URL = getRequiredVar("RSERVE_URL");


  // use in-memory test DB unless "real" application DB is configured
  private static boolean USE_IN_MEMORY_TEST_DATABASE = true;

  public Resources(Options opts) {
    super(opts);

    // initialize auth and required DBs
    DbManager.initUserDatabase(opts);
    DbManager.initAccountDatabase(opts);
    enableAuth();

    if (opts.getAppDbOpts().name().isPresent() ||
        opts.getAppDbOpts().tnsName().isPresent()) {
      // application database configured; use it
      USE_IN_MEMORY_TEST_DATABASE = false;
    }

    if (ENV.isDevelopmentMode()) {
//      enableJerseyTrace();
//      ClientUtil.LOG_RESPONSE_HEADERS = true;
    }

    if (!USE_IN_MEMORY_TEST_DATABASE) {
      DbManager.initApplicationDatabase(opts);
      LOG.info("Using application DB connection URL: " +
          DbManager.getInstance().getApplicationDatabase().getConfig().getConnectionUrl());
    }
  }

  public static MetadataCache getMetadataCache() {
    return METADATA_CACHE;
  }

  public static boolean isFileBasedSubsettingEnabled() {
    return ENV.isFileBasedSubsettingEnabled();
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
    return USE_IN_MEMORY_TEST_DATABASE ? "" : ENV.getAppDbSchema();
  }

  public static String getUserStudySchema() {
    return USE_IN_MEMORY_TEST_DATABASE ? "" : ENV.getUserStudySchema();
  }

  public static Path getBinaryFilesDirectory() {
    return Path.of(ENV.getBinaryFilesMount(), ENV.getBinaryFilesDirectory().replace("%DB_BUILD%", ENV.getDbBuild()));
  }

  public static ExecutorService getFileChannelThreadPool() {
    return FILE_READ_THREAD_POOL;
  }

  public static ExecutorService getDeserializerThreadPool() {
    return DESERIALIZER_THREAD_POOL;
  }

  public static String getDatasetAccessServiceUrl() {
    return ENV.getDatasetAccessServiceUrl();
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
        // DataService
        AppsService.class,
        FilterAwareMetadataService.class,
        // Merging
        MergingServiceExternal.class,
        MergingServiceInternal.class
    };
  }
}
