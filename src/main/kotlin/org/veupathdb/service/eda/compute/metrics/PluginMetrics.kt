package org.veupathdb.service.eda.compute.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Histogram

/**
 * Prometheus Metrics for Plugins
 *
 * These metrics add to the existing metrics exposed by the async platform.
 *
 * @author Elizabeth Paige Harper - https://github.com/foxcapades
 * @since 1.0.0
 */
object PluginMetrics {

  /**
   * Histogram of plugin execution time.
   *
   * Time is measured in seconds.
   *
   * **NOTE**: This is not the full job execution time, this is specifically the
   * time spent by the plugin implementation code.  In other words, time spent
   * between when this job is popped from the queue and when the plugin code
   * is executed is not measured by this metric.
   */
  val execTime: Histogram = Histogram.build()
    .buckets(
      0.1,
      0.5,
      1.0,
      1.5,
      3.0,
      5.0,
      10.0,
      15.0,
      30.0,
      60.0,
      120.0,
      180.0,
      300.0,
      3000.0,
    )
    .labelNames("plugin_name")
    .name("plugin_exec_time")
    .help("Plugin execution time in seconds.")
    .register()

  /**
   * Counter of successful plugin executions.
   *
   * This differs from the async platform's `job_successes` metric in that it is
   * keyed on plugin name rather than queue name.
   */
  val successes: Counter = Counter.build()
    .labelNames("plugin_name")
    .name("plugin_successes")
    .help("Number of successful executions of a plugin")
    .register()

  /**
   * Counter of failed plugin executions.
   *
   * This differs from the async platform's `job_failures` metric in that it is
   * keyed on the plugin name rather than the queue name.
   */
  val failures: Counter = Counter.build()
    .labelNames("plugin_name")
    .name("plugin_failures")
    .help("Number of failed executions of a plugin")
    .register()
}