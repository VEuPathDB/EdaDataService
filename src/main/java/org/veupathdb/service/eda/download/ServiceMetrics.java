package org.veupathdb.service.eda.download;

import io.prometheus.client.Counter;

/**
 * Utility class for emitting domain-specific service metrics.
 */
public class ServiceMetrics {
  private static final String STUDY_LABEL = "study";
  private static final String USER_LABEL = "user";
  private static final String RESOURCE_LABEL = "resource";

  private static final Counter STUDY_FILE_DOWNLOAD_METRIC = Counter.build()
      .name("dataset_download_requested")
      .help("Dataset download request count")
      .labelNames(STUDY_LABEL, USER_LABEL, RESOURCE_LABEL)
      .register();

  public static void reportDownloadCount(String datasetId, String userId, String resourceName) {
    STUDY_FILE_DOWNLOAD_METRIC.labels(datasetId, userId, resourceName).inc();
  }
}
