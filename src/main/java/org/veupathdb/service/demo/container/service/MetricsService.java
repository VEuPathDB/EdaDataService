package org.veupathdb.service.demo.container.service;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import org.veupathdb.service.demo.generated.resources.Metrics;

import javax.ws.rs.core.StreamingOutput;

import java.io.OutputStreamWriter;

public class MetricsService implements Metrics {
  @Override
  public GetMetricsResponse getMetrics() {
    return GetMetricsResponse.respond200WithTextPlain(metricsWriter());
  }

  static StreamingOutput metricsWriter() {
    return output -> {
      try (var write = new OutputStreamWriter(output)) {
        TextFormat.write004(write, CollectorRegistry.defaultRegistry.metricFamilySamples());
      }
    };
  }
}
