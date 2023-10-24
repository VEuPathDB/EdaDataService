package org.veupathdb.service.eda.compute.plugins

import org.apache.logging.log4j.LogManager
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec
import org.veupathdb.service.eda.common.plugin.util.PluginUtil
import org.veupathdb.service.eda.compute.jobs.ReservedFiles
import org.veupathdb.service.eda.compute.metrics.PluginMetrics
import org.veupathdb.service.eda.generated.model.APIStudyDetail
import org.veupathdb.service.eda.generated.model.ComputeRequestBase

/**
 * Abstract Plugin Base
 *
 * Base implementation of the [Plugin] interface that defines and provides
 * access to properties and utilities to assist in the development and
 * implementation of plugins.
 *
 * Plugin implementations must have a constructor that takes at least the plugin
 * context as an argument.  Plugins must also implement the [execute] method
 * which is called internally by this class' implementation of [Plugin.run].
 *
 * In addition to defining the internal [execute] method, this abstract type
 * defines, and provides values for the following properties:
 *
 * * [config]      - The plugin's configuration object.
 * * [rawRequest]  - The original request sent in to the service which triggered
 *   the job to be run.
 * * [studyDetail] - [APIStudyDetail] data fetched from the EDA Subsetting
 *   Service for this plugin execution.
 * * [workspace]   - Access to the local scratch workspace for the plugin.  This
 *   is where the plugin will access its tabular data from as well as where it
 *   may write/read any arbitrary data needed to perform its tasks.
 *
 * @author Elizabeth Paige Harper - https://github.com/Foxcapades
 * @since 1.0.0
 */
abstract class AbstractPlugin<R : ComputeRequestBase, C>(
  /**
   * Context in/for which this plugin is being executed.
   */
  protected val context: PluginContext<R, C>
) : Plugin {

  private val Log = LogManager.getLogger(javaClass)

  // ╔════════════════════════════════════════════════════════════════════╗ //
  // ║                                                                    ║ //
  // ║  Abstract Extension Points                                         ║ //
  // ║                                                                    ║ //
  // ║  Methods and values which must be implemented by the extending     ║ //
  // ║  plugin.                                                           ║ //
  // ║                                                                    ║ //
  // ╚════════════════════════════════════════════════════════════════════╝ //

  /**
   * Executes this plugin's tasks.
   *
   * Plugins can indicate an execution failure or bad status by throwing an
   * exception.  Any exception thrown by this method will result in the job
   * being marked as "failed".
   *
   * Additionally, the exception thrown will be written to file and persisted
   * to the job's workspace in the remote cache for debugging purposes.  It is
   * recommended that the exceptions thrown be descriptive or have useful
   * messages as they will be used to debug plugin job failures.
   */
  protected abstract fun execute()

  // ╔════════════════════════════════════════════════════════════════════╗ //
  // ║                                                                    ║ //
  // ║  Internal API                                                      ║ //
  // ║                                                                    ║ //
  // ║  Methods and values provided for convenience of implementation     ║ //
  // ║  for extending plugins.                                            ║ //
  // ║                                                                    ║ //
  // ╚════════════════════════════════════════════════════════════════════╝ //

  /**
   * Input configuration.
   *
   * This is the configuration segment of the original request body sent in to
   * the service.
   */
  protected val config
    get() = context.config

  /**
   * Raw input request.
   *
   * This is the full original request body sent in to the service.
   */
  protected val rawRequest
    get() = context.request

  /**
   * Study details.
   */
  protected val studyDetail
    get() = context.studyDetail

  /**
   * Plugin workspace.
   */
  protected val workspace
    get() = context.workspace

  /**
   * EDA Common [PluginUtil] Instance
   */
  protected val util
    get() = PluginUtil(context.referenceMetadata, context.mergingClient)

  // ╔════════════════════════════════════════════════════════════════════╗ //
  // ║                                                                    ║ //
  // ║  Final Public API                                                  ║ //
  // ║                                                                    ║ //
  // ║  Methods and values which must be implemented by the extending     ║ //
  // ║  plugin.                                                           ║ //
  // ║                                                                    ║ //
  // ╚════════════════════════════════════════════════════════════════════╝ //

  final override fun run(): Boolean {
    Log.info("Executing plugin {}", { context.pluginMeta.urlSegment })

    try {
      // Start a timer to time the plugin execution.
      val tim = PluginMetrics.execTime
        .labels(context.pluginMeta.urlSegment)
        .startTimer()

      // execute the plugin
      execute()

      // Record the plugin execution time in the metrics
      tim.observeDuration()

      // Record the job success in the metrics
      PluginMetrics.successes.labels(context.pluginMeta.urlSegment).inc()

      return true
    } catch (e: Throwable) {
      Log.error("Plugin execution failed", e)

      // Record the job failure in the metrics
      PluginMetrics.failures.labels(context.pluginMeta.urlSegment).inc()

      // Write the stacktrace to file to be persisted in S3
      val eWriter = context.workspace.touch(ReservedFiles.OutputException).toFile().printWriter()
      e.printStackTrace(eWriter)
      eWriter.flush()
      eWriter.close()

      return false
    }
  }
}
