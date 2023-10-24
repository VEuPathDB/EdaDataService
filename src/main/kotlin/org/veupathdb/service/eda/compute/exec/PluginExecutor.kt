package org.veupathdb.service.eda.compute.exec

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.ThreadContext
import org.veupathdb.lib.compute.platform.job.JobContext
import org.veupathdb.lib.compute.platform.job.JobExecutor
import org.veupathdb.lib.compute.platform.job.JobResult
import org.veupathdb.lib.jackson.Json
import org.veupathdb.service.eda.common.client.EdaMergingClient
import org.veupathdb.service.eda.compute.EDA
import org.veupathdb.service.eda.compute.jobs.ReservedFiles
import org.veupathdb.service.eda.compute.plugins.Plugin
import org.veupathdb.service.eda.compute.plugins.AbstractPlugin
import org.veupathdb.service.eda.compute.plugins.PluginRegistry
import org.veupathdb.service.eda.compute.plugins.PluginWorkspace
import org.veupathdb.service.eda.compute.service.ServiceOptions

/**
 * Standard set of files we will attempt to persist to S3 on "successful" job
 * completion.
 */
private val OutputFiles = arrayOf(
  ReservedFiles.OutputMeta,
  ReservedFiles.OutputStats,
  ReservedFiles.OutputTabular,
  ReservedFiles.OutputErrors,
  ReservedFiles.OutputException
)

// These MUST match the %X{} variables defined in resources/log4j2.yml
private const val ThreadContextJobKey = "JOB_ID"
private const val ThreadContextPluginKey = "PLUGIN"

/**
 * # Plugin Executor
 *
 * Implementation of [JobExecutor] wrapping the execution of an EDA Compute
 * plugin.
 *
 * @author Elizabeth Paige Harper - https://github.com/foxcapades
 * @since 1.0.0
 */
class PluginExecutor : JobExecutor {

  private val Log = LogManager.getLogger(javaClass)

  /**
   * # Plugin Execution
   *
   * This method prepares the service and a target plugin for execution,
   * executes that plugin, then persists the plugin's outputs to a cache in an
   * external S3 store.
   *
   * ## 1. Preparation
   *
   * Preparing for a plugin execution includes the following steps:
   *
   * * Configuring the Log4j2 ThreadContext with the plugin name and job ID to
   *   allow us to follow the execution of a specific job in the logs for
   *   debugging purposes.
   * * Deserialize the job payload (again) to hand to the plugin to execute.
   * * Fetch the APIStudyDetail information from the EDA Subsetting Service to
   *   hand to the plugin as an input.
   * * Write the APIStudyDetail information to file in the job/plugin's local
   *   scratch workspace.
   * * Fetch all the tabular data required by the plugin from the EDA Merge
   *   Service to hand to the plugin as inputs.
   * * Write the tabular data to files in the job/plugin's local scratch
   *   workspace.
   *
   * ## 2. Execution
   *
   * Once the preparation steps have been completed the plugin itself will be
   * executed with the input job configuration.
   *
   * The [Plugin.run] method promises not to throw exceptions.  Instead, the
   * `run` method returns a boolean value indicating whether the execution was
   * successful.
   *
   * ## 3. Persistence
   *
   * ### 3.a. On Success
   *
   * When a plugin completes successfully, there is a known set of output files
   * that may be persisted.  The post-execution persistence step will attempt to
   * persist all of those files (only actually persisting the files that exist).
   *
   * The set of known files to persist is defined by [OutputFiles].
   *
   * ### 3.b. On Failure
   *
   * When a plugin completes unsuccessfully, we attempt to persist the entire
   * local scratch workspace to the external S3 store to assist in debugging the
   * job failure.
   *
   * Since a plugin may create any number of arbitrary files or directories, we
   * must first figure out what files there are to persist, then go through and
   * attempt to persist all of them to the cache.
   */
  override fun execute(ctx: JobContext): JobResult {
    // Add the job id to the logger thread context so log messages in this
    // worker thread will be marked as part of this job.
    ThreadContext.put(ThreadContextJobKey, ctx.jobID.string)
    Log.info("Executing job {}", ctx.jobID)

    // Parse the raw job payload from the queue message
    val jobPayload = Json.parse<PluginJobPayload>(ctx.config!!)

    // Get the plugin provider for the job
    val provider = PluginRegistry.get(jobPayload.plugin)!!

    // Add the plugin name to the logging context
    ThreadContext.put(ThreadContextPluginKey, provider.urlSegment)
    Log.debug("loaded plugin provider")

    // Deserialize the
    val request = Json.parse(jobPayload.request, provider.requestClass)

    // Convert the auth header to a Map.Entry for use with eda-common code.
    val authHeader = jobPayload.authHeader.toFgpTuple()

    // Fetch the study metadata and write it out to the local workspace
    val studyDetail = try {
      Log.debug("retrieving api study details")
      EDA.requireAPIStudyDetail(request.studyId, authHeader)
        .also { ctx.workspace.write(ReservedFiles.InputMeta, Json.convert(it)) }
    } catch (e: Throwable) {
      Log.error("Failed to fetch APIStudyDetail.", e)
      e.printStackTrace(ctx.workspace.touch(ReservedFiles.OutputException).toFile().printWriter())
      return JobResult.failure(*OutputFiles)
    }

    // Build the plugin context
    val context = provider.getContextBuilder().also {
      it.studyDetail = studyDetail
      it.request = request
      it.workspace = PluginWorkspace(ctx.workspace)
      it.jobContext = ComputeJobContext(ctx.jobID)
      it.pluginMeta = provider
      it.mergingClient = EdaMergingClient(ServiceOptions.edaMergeHost, authHeader)
    }.build()

    // Create the plugin.
    val plugin = provider.createPlugin(context)

    // Validate the stream specs
    if (!validateStreamSpecs(plugin))
    // If the stream specs were not valid, exit here with a failed status.
      return JobResult.failure(*OutputFiles)


    try {
      // Fetch the tabular data and write it out to the local workspace
      plugin.streamSpecs.forEach { spec ->
        Log.debug("retrieving tabular study data: {}", spec.streamName)
        ctx.workspace.write(
          spec.streamName, EDA.getMergeData(
            context.referenceMetadata,
            request.filters,
            request.derivedVariables,
            spec,
            authHeader
          )
        )
      }
    } catch (e: Throwable) {
      Log.error("Failed to fetch tabular data.", e)
      e.printStackTrace(ctx.workspace.touch(ReservedFiles.OutputException).toFile().printWriter())
      return JobResult.failure(*OutputFiles)
    }

    // Execute the plugin
    Log.debug("running plugin")
    return if (plugin.run()) {
      // If the plugin executed successfully, look for the known target files to
      // persist to S3.
      JobResult.success(*OutputFiles)
    } else {
      // If the plugin execution failed, try and persist everything in the local
      // workspace.
      JobResult.failure(ctx.workspace.path.toFile().listFiles()!!.map { it.name })
    }
  }

  private fun validateStreamSpecs(plugin: AbstractPlugin<*, *>): Boolean {
    Log.debug("validating plugin StreamSpecs")

    var valid = true

    plugin.streamSpecs.forEach {
      if (ReservedFiles.isReservedFileName(it.streamName)) {
        Log.error("Plugin is attempting to download merge data into reserved file \"{}\"", it.streamName)
        valid = false
      }
    }

    return valid
  }
}
