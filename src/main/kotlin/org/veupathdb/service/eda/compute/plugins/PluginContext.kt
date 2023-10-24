package org.veupathdb.service.eda.compute.plugins

import org.veupathdb.service.eda.common.client.EdaMergingClient
import org.veupathdb.service.eda.common.model.ReferenceMetadata
import org.veupathdb.service.eda.compute.exec.ComputeJobContext
import org.veupathdb.service.eda.compute.process.ComputeProcessBuilder
import org.veupathdb.service.eda.generated.model.APIStudyDetail
import org.veupathdb.service.eda.generated.model.ComputeRequestBase
import kotlin.reflect.full.memberFunctions

/**
 * Plugin Execution Context
 *
 * Provides the context for a plugin execution.
 *
 * The execution context includes:
 *
 * * The original HTTP request that trigger the job to be queued.
 * * A handle on the local scratch workspace where files can be read/written.
 * * Additional context from the compute service about the current job.
 * * Accessors for:
 *     * The API study details
 *     * The Tabular merge data for the job
 *
 * @author Elizabeth Paige Harper - https://github.com/foxcapades
 * @since 1.0.0
 */
interface PluginContext<R: ComputeRequestBase, C> {

  /**
   * Meta information about the current plugin.
   */
  val pluginMeta: PluginMeta<R>

  /**
   * Original HTTP request that was sent to the compute service to kick off the
   * current job.
   */
  val request: R

  /**
   * Job configuration.
   *
   * Alias for `request.config` or `getRequest().getConfig()`
   */
  val config: C

  /**
   * A handle on the workspace the plugin is being executed in.
   */
  val workspace: PluginWorkspace

  /**
   * Additional compute service context for the job being executed.
   */
  val jobContext: ComputeJobContext

  /**
   * API Study Details
   */
  val studyDetail: APIStudyDetail

  /**
   * Reference Metadata
   */
  val referenceMetadata: ReferenceMetadata

  /**
   * EDA Merging Client Instance
   */
  val mergingClient: EdaMergingClient

  /**
   * Returns a new [ComputeProcessBuilder] which may be used to construct an
   * external process execution and run that external process.
   *
   * @param command Name of/absolute path to the external binary or script that
   * will be executed.
   *
   * @return A new [ComputeProcessBuilder] instance.
   */
  fun processBuilder(command: String, vararg args: String): ComputeProcessBuilder
}


/**
 * Plugin Execution Context Builder
 *
 * Builder for constructing a new [PluginContext] instance.
 *
 * This type is mostly here to work around the fact that the generic types are
 * completely unknown to the plugin executor and this allows the safe
 * construction of a [PluginContext] instance without needing to rely on
 * unsafe casts.
 *
 * @author Elizabeth Paige Harper - https://github.com/foxcapades
 * @since 1.0.0
 */
class PluginContextBuilder<R : ComputeRequestBase, C> {
  var request: R? = null

  var workspace: PluginWorkspace? = null

  var jobContext: ComputeJobContext? = null

  var pluginMeta: PluginMeta<R>? = null

  var studyDetail: APIStudyDetail? = null

  var mergingClient: EdaMergingClient? = null

  fun request(request: R): PluginContextBuilder<R, C> {
    this.request = request
    return this
  }

  fun workspace(workspace: PluginWorkspace): PluginContextBuilder<R, C> {
    this.workspace = workspace
    return this
  }

  fun jobContext(jobContext: ComputeJobContext): PluginContextBuilder<R, C> {
    this.jobContext = jobContext
    return this
  }

  fun pluginMeta(pluginMeta: PluginMeta<R>): PluginContextBuilder<R, C> {
    this.pluginMeta = pluginMeta
    return this
  }

  fun studyDetail(studyDetail: APIStudyDetail): PluginContextBuilder<R, C> {
    this.studyDetail = studyDetail
    return this
  }

  fun mergingClient(mergingClient: EdaMergingClient): PluginContextBuilder<R, C> {
    this.mergingClient = mergingClient
    return this
  }

  fun build(): PluginContext<R, C> =
    PluginContextImpl(
      request       ?: throw IllegalStateException("request must not be null"),
      workspace     ?: throw IllegalStateException("workspace must not be null"),
      jobContext    ?: throw IllegalStateException("jobContext must not be null"),
      pluginMeta    ?: throw IllegalStateException("pluginMeta must not be null"),
      studyDetail   ?: throw IllegalStateException("studyDetail must not be null"),
      mergingClient ?: throw IllegalStateException("mergingClient must not be null"),
    )
}


private class PluginContextImpl<R : ComputeRequestBase, C>(
  override val request: R,
  override val workspace: PluginWorkspace,
  override val jobContext: ComputeJobContext,
  override val pluginMeta: PluginMeta<R>,
  override val studyDetail: APIStudyDetail,
  override val mergingClient: EdaMergingClient
) : PluginContext<R, C> {

  @Suppress("UNCHECKED_CAST")
  override val config: C by lazy {
    (request::class.memberFunctions.find { it.name == "getConfig" }
      ?: throw NoSuchMethodException("Request type ${request::class.java} does not have a 'getConfig' method."))
      .call(request) as C
  }

  override val referenceMetadata: ReferenceMetadata by lazy {
    // IDE may show this line as a compile error, but it is not, the generated
    // classes are resolved at compile time.
    val meta = ReferenceMetadata(this.studyDetail)
    val derivedVars = request.derivedVariables ?: emptyList()
    if (derivedVars.isNotEmpty())
      this.mergingClient.getDerivedVariableMetadata(meta.studyId, derivedVars).forEach {
        meta.incorporateDerivedVariable(it)
      }
    meta
  }

  override fun processBuilder(command: String, vararg args: String) =
    ComputeProcessBuilder(command, workspace.path)
      .addArgs(*args)
}
