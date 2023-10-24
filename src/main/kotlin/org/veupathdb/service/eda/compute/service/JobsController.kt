package org.veupathdb.service.eda.compute.service

import jakarta.ws.rs.ForbiddenException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.core.StreamingOutput
import org.veupathdb.lib.compute.platform.AsyncPlatform
import org.veupathdb.lib.compute.platform.job.JobFileReference
import org.veupathdb.lib.container.jaxrs.server.annotations.Authenticated
import org.veupathdb.lib.hash_id.HashID
import org.veupathdb.service.eda.compute.util.toJobResponse
import org.veupathdb.service.eda.generated.resources.Jobs

@Authenticated(allowGuests = false)
class JobsController : Jobs {

  /**
   * Lookup job by job id.
   */
  override fun getJobsByJobId(jobId: String) =
    Jobs.GetJobsByJobIdResponse.respond200WithApplicationJson(requireJob(jobId).toJobResponse())!!

  /**
   * List files for job by job id.
   */
  override fun getJobsFilesByJobId(jobId: String) =
    Jobs.GetJobsFilesByJobIdResponse.respond200WithApplicationJson(fileList(jobId).map(JobFileReference::name))!!

  /**
   * Get the contents of a file by job id and file name.
   */
  override fun getJobsFilesByJobIdAndFileName(jobId: String, fileName: String) =
    fileList(jobId)
      .let { it.find { it.name == fileName } }
      ?.let { StreamingOutput { o -> it.open().use { i -> i.transferTo(o) } } }
      ?.let { Jobs.GetJobsFilesByJobIdAndFileNameResponse.respond200WithTextPlain(it,
        Jobs.GetJobsFilesByJobIdAndFileNameResponse.headersFor200().withContentDisposition("attachment; filename=$fileName")) }
      ?: throw NotFoundException()

  /**
   * Delete a job by job id.
   */
  override fun deleteJobsByJobId(rawId: String) =
    requireJob(rawId)
      .also { it.owned && it.status.isFinished || throw ForbiddenException() }
      .also { AsyncPlatform.deleteJob(it.jobID) }
      .let { Jobs.DeleteJobsByJobIdResponse.respond204()!! }

  @Suppress("NOTHING_TO_INLINE")
  private inline fun requireJob(rawID: String) =
    AsyncPlatform.getJob(rawID.toHashID()) ?: throw NotFoundException()

  @Suppress("NOTHING_TO_INLINE")
  private inline fun fileList(rawID: String) =
    requireJob(rawID).let { AsyncPlatform.getJobFiles(it.jobID) }

  @Suppress("NOTHING_TO_INLINE")
  private inline fun String.toHashID(): HashID {
    return try {
      // Attempt to parse the raw string as a HashID
      HashID(this)
    } catch (e: IllegalArgumentException) {
      // If it is not a valid hash ID string, throw a not found exception as
      // there are no jobs that can be located with an invalid ID.
      throw NotFoundException()
    }
  }
}