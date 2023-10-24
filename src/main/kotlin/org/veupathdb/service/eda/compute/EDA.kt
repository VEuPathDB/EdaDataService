package org.veupathdb.service.eda.compute

import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import org.apache.logging.log4j.LogManager
import org.gusdb.fgputil.Tuples.TwoTuple
import org.veupathdb.lib.compute.platform.AsyncPlatform
import org.veupathdb.lib.compute.platform.job.JobFileReference
import org.veupathdb.lib.jackson.Json
import org.veupathdb.service.eda.common.auth.StudyAccess
import org.veupathdb.service.eda.common.client.DatasetAccessClient
import org.veupathdb.service.eda.common.client.EdaMergingClient
import org.veupathdb.service.eda.common.client.EdaSubsettingClient
import org.veupathdb.service.eda.common.client.spec.StreamSpec
import org.veupathdb.service.eda.common.model.ReferenceMetadata
import org.veupathdb.service.eda.compute.exec.PluginJobPayload
import org.veupathdb.service.eda.compute.plugins.PluginMeta
import org.veupathdb.service.eda.compute.service.ServiceOptions
import org.veupathdb.service.eda.compute.util.JobIDs
import org.veupathdb.service.eda.compute.util.toAuthTuple
import org.veupathdb.service.eda.compute.util.toJobResponse
import org.veupathdb.service.eda.generated.model.*
import java.io.InputStream
import java.util.*
import org.veupathdb.lib.compute.platform.job.JobStatus as JS

/**
 * EDA Project/Service Access Singleton
 *
 * Home location for single functions used to work with various EDA services
 * including the compute service.
 *
 *
 */
object EDA {

  private val Log = LogManager.getLogger(javaClass)

  /**
   * Returns the [StudyAccess] permissions object for the target [studyID] and
   * user described by the given [auth] header.
   *
   * @param studyID ID of the study for which the target user's permissions
   * should be fetched.
   *
   * @param auth Auth header sent in with the HTTP request describing the user
   * whose permissions should be fetched.
   *
   * @return `StudyAccess` permissions object describing the permissions the
   * target user has for the target study.
   */
  @JvmStatic
  fun getStudyPerms(studyID: String, auth: TwoTuple<String, String>): StudyAccess =
    DatasetAccessClient(ServiceOptions.datasetAccessHost, auth).getStudyAccessByStudyId(studyID)
      .orElseThrow { ForbiddenException() }

  /**
   * Fetches the [APIStudyDetail] information from the EDA Subsetting Service
   * for the given [studyID] if such a study exists.
   *
   * @param studyID ID of the study whose metadata should be retrieved.
   *
   * @param auth Auth header sent in with the job HTTP request.
   *
   * @return An [Optional] that will wrap the `APIStudyDetail` returned from the
   * EDA Subsetting Service, if such a study exists.  If the target study was
   * not found, the returned `Optional` will be empty.
   */
  @JvmStatic
  fun getAPIStudyDetail(studyID: String, auth: TwoTuple<String, String>): Optional<APIStudyDetail> =
    EdaSubsettingClient(ServiceOptions.edaSubsettingHost, auth).getStudy(studyID)

  /**
   * Fetches the [APIStudyDetail] information from the EDA Subsetting Service
   * for the given [studyID], throwing an exception if no such study exists.
   *
   * @param studyID ID of the study whose metadata should be retrieved.
   *
   * @param auth Auth header sent in with the job HTTP request.
   *
   * @param err Optional exception provider that will be used to get the
   * exception that will be thrown if the target study does not exist.
   *
   * @return The `APIStudyDetail` information returned from the EDA Subsetting
   * Service.
   *
   * @throws Exception Throws the exception returned by the given exception
   * provider ([err]).
   */
  @JvmStatic
  @JvmOverloads
  fun requireAPIStudyDetail(
    studyID: String,
    auth: TwoTuple<String, String>,
    err: (studyID: String) -> Exception = this::noStudyDetail
  ): APIStudyDetail = getAPIStudyDetail(studyID, auth).orElseThrow { err(studyID) }

  /**
   * Fetches tabular study data from the EDA Merge Service for the given params.
   *
   * @param refMeta reference metadata about the EDA study whose data is being computed
   *
   * @param filters set of filters to determine the current subset
   *
   * @param spec specification of tabular data stream needed from subsetting service (entity + vars)
   *
   * @param auth Auth header sent in with the job HTTP request.
   *
   * @return An [InputStream] over the tabular data returned from the EDA Merge
   * Service.
   */
  @JvmStatic
  fun getMergeData(
    refMeta: ReferenceMetadata,
    filters: List<APIFilter>,
    derivedVars: List<DerivedVariableSpec>,
    spec: StreamSpec,
    auth: TwoTuple<String, String>
  ): InputStream =
    EdaMergingClient(ServiceOptions.edaMergeHost, auth)
      .getTabularDataStream(refMeta, filters, derivedVars, Optional.empty(), spec).inputStream

  /**
   * Submits a new compute job to the queue.
   *
   * @param plugin Plugin details.
   *
   * @param payload HTTP request containing the compute config.
   *
   * @param auth: Auth header sent in with the job HTTP request.
   *
   * @param autostart: Whether to start a job that does not yet exist.
   *
   * @return A response describing the job that was created.
   */
  @JvmStatic
  fun <R : ComputeRequestBase> getOrSubmitComputeJob(
    plugin: PluginMeta<R>,
    payload: R,
    auth: TwoTuple<String, String>,
    autostart: Boolean,
  ): JobResponse {

    // Create job ID by hashing plugin name and compute config
    val jobID = JobIDs.of(plugin.urlSegment, payload)

    // Lookup the job to see if it already exists
    val existingJob = AsyncPlatform.getJob(jobID)

    // If the job already exists
    if (existingJob != null)
      // And the status is not expired OR autostart is false
      if (existingJob.status != JS.Expired || !autostart)
        // Just return the job
        return existingJob.toJobResponse()

    // If we made it here, the job either does not exist, or exists and is
    // expired.

    // If autostart is true then submit the job for (re)execution
    if (autostart) {
      Log.info("Submitting job {} to the queue", jobID)

      // Build the rabbitmq message payload
      val jobPay = PluginJobPayload(plugin.urlSegment, Json.convert(payload), auth.toAuthTuple())

      // Submit the job
      AsyncPlatform.submitJob(plugin.targetQueue.queueName) {
        this.jobID = jobID
        this.config = Json.convert(jobPay)
      }

      // Return the job ID with queued status (even though it may have moved to another status already)
      return JobResponseImpl().also {
        it.jobID = jobID.string
        it.status = JobStatus.QUEUED
      }
    }

    // Return the job ID with no-such-job status
    return JobResponseImpl().also {
      it.jobID = jobID.string
      it.status = JobStatus.NOSUCHJOB
    }
  }

  @JvmStatic
  fun getComputeJobFiles(plugin: PluginMeta<out ComputeRequestBase>, payload: ComputeRequestBase): List<JobFileReference> {
    val jobID = JobIDs.of(plugin.urlSegment, payload)

    // Get the target job (or throw 404) and ensure that it is finished (or
    // throw 403)
    (AsyncPlatform.getJob(jobID) ?: throw NotFoundException())
      .status
      .isFinished || throw ForbiddenException()

    return AsyncPlatform.getJobFiles(jobID)
  }


  @JvmStatic
  private fun noStudyDetail(studyID: String) =
    Exception("Could not get APIStudyDetail for study $studyID")
}
