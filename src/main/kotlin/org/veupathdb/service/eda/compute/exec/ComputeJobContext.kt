package org.veupathdb.service.eda.compute.exec

import org.veupathdb.lib.hash_id.HashID

/**
 * Container for additional service level details about a job.
 *
 * Provided to plugins as part of a job execution context to grant access to
 * details about the job that are not exposed to the job via the job's
 * configuration.
 *
 * This is where properties and fields should be added when attempting to pass
 * service level information into a plugin's execution context.
 *
 * @author Elizabeth Paige Harper - https://github.com/Foxcapades
 * @since 1.0.0
 */
class ComputeJobContext(
  /**
   * Hash ID of the job being executed.
   */
  val jobID: HashID
)