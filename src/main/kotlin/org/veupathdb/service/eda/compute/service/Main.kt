package org.veupathdb.service.eda.compute.service

import org.veupathdb.lib.compute.platform.AsyncPlatform
import org.veupathdb.lib.compute.platform.job.JobExecutorFactory
import org.veupathdb.lib.container.jaxrs.config.Options
import org.veupathdb.lib.container.jaxrs.server.ContainerResources
import org.veupathdb.lib.container.jaxrs.server.Server
import org.veupathdb.service.eda.compute.exec.PluginExecutor

object Main : Server() {
  @JvmStatic
  fun main(args: Array<String>) {
    this.enableAccountDB()
    this.enableUserDB()

    this.start(args)
  }

  override fun newResourceConfig(opts: Options?): ContainerResources {
    val out = Resources()

    // Enabled by default for debugging purposes, this should be removed when
    // production ready.
    out.property("jersey.config.server.tracing.type", "ALL")
      .property("jersey.config.server.tracing.threshold", "VERBOSE")

    return out
  }

  override fun newOptions() = ServiceOptions

  override fun postCliParse(opts: Options) = initAsyncPlatform()

  private fun initAsyncPlatform() {
    AsyncPlatform.init {
      dbConfig {
        dbName   = ServiceOptions.queueDBName
        host     = ServiceOptions.queueDBHost
        port     = ServiceOptions.queueDBPort
        username = ServiceOptions.queueDBUsername
        password = ServiceOptions.queueDBPassword
        poolSize = ServiceOptions.queueDBPoolSize
      }

      s3Config {
        host        = ServiceOptions.s3Host
        bucket      = ServiceOptions.s3Bucket
        accessToken = ServiceOptions.s3AccessToken
        secretKey   = ServiceOptions.s3SecretKey
        port        = ServiceOptions.s3Port
        https       = ServiceOptions.s3UseHttps
      }

      jobConfig {
        executorFactory = JobExecutorFactory { PluginExecutor() }
        expirationDays  = ServiceOptions.jobCacheTimeoutDays
      }

      addQueue {
        id       = ServiceOptions.slowQueueName
        username = ServiceOptions.jobQueueUsername
        password = ServiceOptions.jobQueuePassword
        host     = ServiceOptions.jobQueueHost
        port     = ServiceOptions.jobQueuePort
        workers  = ServiceOptions.slowQueueWorkers
      }

      addQueue {
        id       = ServiceOptions.fastQueueName
        username = ServiceOptions.jobQueueUsername
        password = ServiceOptions.jobQueuePassword
        host     = ServiceOptions.jobQueueHost
        port     = ServiceOptions.jobQueuePort
        workers  = ServiceOptions.fastQueueWorkers
      }
    }
  }
}