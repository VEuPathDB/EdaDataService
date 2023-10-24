package org.veupathdb.service.eda.subset.service;

import io.prometheus.client.Counter;

public class ServiceMetrics {
  private static Counter SUBSET_DOWNLOAD_REQUESTED = Counter.build()
      .name("subset_download_requested")
      .labelNames("study_name", "user_id", "entity")
      .help("Counter indicating number of subset downloads requested")
      .register();

  public static void reportSubsetDownload(String studyName, String userId, String entity) {
    SUBSET_DOWNLOAD_REQUESTED.labels(studyName, userId, entity).inc();
  }
}
