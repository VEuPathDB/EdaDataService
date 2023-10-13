package org.veupathdb.service.eda.compute.plugins

import org.veupathdb.service.eda.compute.service.ServiceOptions

/**
 * Plugin Queue
 *
 * Defines the different queues that jobs for plugins may be submitted to.
 *
 * Plugins define which queue their jobs should be sorted to based on the
 * expected job execution time.
 *
 * TODO: Define "fast" vs "slow", what is the threshold at which a plugin should
 *       be considered "slow"?
 *
 * The queues are separated so as not to block quick jobs by a long queue of
 * slower jobs.
 *
 * @author Elizabeth Paige Harper - https://github.com/foxcapades
 * @since 1.0.0
 */
enum class PluginQueue {

  /**
   * "Fast" plugin queue.
   *
   * This queue is intended to be used for plugins whose jobs are expected to
   * be executed quickly.
   */
  Fast,

  /**
   * "Slow" plugin queue.
   *
   * This queue is intended to be used for plugins whose jobs are expected to
   * take some time to complete.
   */
  Slow;

  /**
   * The job-queue name for this [PluginQueue] value.
   */
  val queueName
    get() = when(this) {
      Fast -> ServiceOptions.fastQueueName
      Slow -> ServiceOptions.slowQueueName
    }
}