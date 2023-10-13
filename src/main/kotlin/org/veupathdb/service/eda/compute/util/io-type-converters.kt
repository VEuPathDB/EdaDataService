@file:JvmName("ConversionUtil")
@file:Suppress("NOTHING_TO_INLINE")

package org.veupathdb.service.eda.compute.util

import org.veupathdb.lib.compute.platform.job.AsyncJob
import org.veupathdb.service.eda.generated.model.JobResponse
import org.veupathdb.service.eda.generated.model.JobResponseImpl
import org.veupathdb.lib.compute.platform.job.JobStatus as IJobStatus
import org.veupathdb.service.eda.generated.model.JobStatus as OJobStatus

internal inline fun AsyncJob.toJobResponse(): JobResponse =
  JobResponseImpl().also {
    it.jobID = jobID.string
    it.status = status.toOutJobStatus()
    it.queuePosition = queuePosition
  }

internal inline fun IJobStatus.toOutJobStatus() =
  when (this) {
    IJobStatus.Queued     -> OJobStatus.QUEUED
    IJobStatus.InProgress -> OJobStatus.INPROGRESS
    IJobStatus.Complete   -> OJobStatus.COMPLETE
    IJobStatus.Failed     -> OJobStatus.FAILED
    IJobStatus.Expired    -> OJobStatus.EXPIRED
  }

