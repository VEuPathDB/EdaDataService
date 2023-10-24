package org.veupathdb.service.eda.compute.service

import org.veupathdb.lib.container.jaxrs.config.Options
import picocli.CommandLine.Option

/*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
  ┃  Static Constants                                                        ┃
\*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

private const val UnconfiguredStringValue = "unconfigured"

// Database Defaults
private const val DefaultQueueDBPort = 5432
private const val DefaultQueueDBPoolSize = 10

// Job Queue Defaults
private const val DefaultJobQueuePort = 5672
private const val DefaultSlowQueueName = "slow-jobs"
private const val DefaultSlowQueueWorkers = 5
private const val DefaultFastQueueName = "fast-jobs"
private const val DefaultFastQueueWorkers = 5


// S3 Defaults
private const val DefaultS3Port     = 80
private const val DefaultS3UseHttps = true

// Job Cache Defaults
private const val DefaultJobCacheTimeoutDays = 30

// RServe Defaults
private const val DefaultRServePort = 6311


/**
 * EDA Compute Service CLI/Env Options
 */
object ServiceOptions : Options() {

  // region Postgres

  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃  Queue PostgreSQL                                                    ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  @Option(
    names = ["--queue-db-name"],
    defaultValue = "\${env:QUEUE_DB_NAME}",
    description = ["Queue database name"],
    arity = "1",
    required = true
  )
  var queueDBName = UnconfiguredStringValue
    private set

  @Option(
    names = ["--queue-db-host"],
    defaultValue = "\${env:QUEUE_DB_HOST}",
    description = ["Queue database hostname"],
    arity = "1",
    required = true
  )
  var queueDBHost = UnconfiguredStringValue
    private set

  @Option(
    names = ["--queue-db-port"],
    defaultValue = "\${env:QUEUE_DB_PORT}",
    description = ["Queue database host port"],
    arity = "1"
  )
  var queueDBPort = DefaultQueueDBPort
    private set

  @Option(
    names = ["--queue-db-username"],
    defaultValue = "\${env:QUEUE_DB_USERNAME}",
    description = ["Queue database username"],
    arity = "1",
    required = true
  )
  var queueDBUsername = UnconfiguredStringValue
    private set

  @Option(
    names = ["--queue-db-password"],
    defaultValue = "\${env:QUEUE_DB_PASSWORD}",
    description = ["Queue database password"],
    arity = "1",
    required = true
  )
  var queueDBPassword = UnconfiguredStringValue
    private set

  @Option(
    names = ["--queue-db-pool-size"],
    defaultValue = "\${env:QUEUE_DB_POOL_SIZE}",
    description = ["Queue database pool size"],
    arity = "1"
  )
  var queueDBPoolSize = DefaultQueueDBPoolSize
    private set

  // endregion Postgres

  // region RabbitMQ

  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃  Queue RabbitMQ                                                      ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  @Option(
    names = ["--job-queue-username"],
    defaultValue = "\${env:JOB_QUEUE_USERNAME}",
    description = ["Username for the RabbitMQ instance"],
    arity = "1",
    required = true
  )
  var jobQueueUsername = UnconfiguredStringValue
    private set

  @Option(
    names = ["--job-queue-password"],
    defaultValue = "\${env:JOB_QUEUE_PASSWORD}",
    description = ["Password for the RabbitMQ instance"],
    arity = "1",
    required = true
  )
  var jobQueuePassword = UnconfiguredStringValue
    private set

  @Option(
    names = ["--job-queue-host"],
    defaultValue = "\${env:JOB_QUEUE_HOST}",
    description = ["Hostname for the RabbitMQ instance."],
    arity = "1",
    required = true
  )
  var jobQueueHost = UnconfiguredStringValue
    private set

  @Option(
    names = ["--job-queue-port"],
    defaultValue = "\${env:JOB_QUEUE_PORT}",
    description = ["Host port for the RabbitMQ instance."],
    arity = "1"
  )
  var jobQueuePort = DefaultJobQueuePort
    private set

  @Option(
    names = ["--slow-queue-name"],
    defaultValue = "\${env:SLOW_QUEUE_NAME}",
    description = ["Name of the slow jobs queue."],
    arity = "1"
  )
  var slowQueueName = DefaultSlowQueueName
    private set

  @Option(
    names = ["--slow-queue-workers"],
    defaultValue = "\${env:SLOW_QUEUE_WORKERS}",
    description = ["Number of worker threads used by the slow job queue."],
    arity = "1"
  )
  var slowQueueWorkers = DefaultSlowQueueWorkers
    private set

  @Option(
    names = ["--fast-queue-name"],
    defaultValue = "\${env:FAST_QUEUE_NAME}",
    description = ["Name of the fast jobs queue."],
    arity = "1"
  )
  var fastQueueName = DefaultFastQueueName
    private set

  @Option(
    names = ["--fast-queue-workers"],
    defaultValue = "\${env:FAST_QUEUE_WORKERS}",
    description = ["Number of worker threads used by the fast job queue."],
    arity = "1"
  )
  var fastQueueWorkers = DefaultFastQueueWorkers
    private set

  // endregion RabbitMQ

  // region Minio (S3)

  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃  Queue S3 Instance                                                   ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  @Option(
    names = ["--s3-host"],
    defaultValue = "\${env:S3_HOST}",
    description = ["S3 instance hostname"],
    arity = "1",
    required = true
  )
  var s3Host = UnconfiguredStringValue
    private set

  @Option(
    names = ["--s3-bucket"],
    defaultValue = "\${env:S3_BUCKET}",
    description = ["S3 bucket name"],
    arity = "1",
    required = true
  )
  var s3Bucket = UnconfiguredStringValue
    private set

  @Option(
    names = ["--s3-access-token"],
    defaultValue = "\${env:S3_ACCESS_TOKEN}",
    description = ["S3 access token"],
    arity = "1",
    required = true
  )
  var s3AccessToken = UnconfiguredStringValue
    private set

  @Option(
    names = ["--s3-secret-key"],
    defaultValue = "\${env:S3_SECRET_KEY}",
    description = ["S3 secret key"],
    arity = "1",
    required = true
  )
  var s3SecretKey = UnconfiguredStringValue
    private set

  @Option(
    names = ["--s3-port"],
    defaultValue = "\${env:S3_PORT}",
    description = ["S3 host port"],
    arity = "1"
  )
  var s3Port = DefaultS3Port
    private set

  @Option(
    names = ["--s3-use-https"],
    defaultValue = "\${env:S3_USE_HTTPS}",
    description = ["Whether the platform should use HTTPS when connecting to S3"],
    arity = "1"
  )
  var s3UseHttps = DefaultS3UseHttps
    private set

  // endregion Minio (S3)

  // region Job Configuration

  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃  Queue Job Configuration                                             ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  @Option(
    names = ["--job-cache-timeout-days"],
    defaultValue = "\${env:JOB_CACHE_TIMEOUT_DAYS}",
    description = ["Number of days a job will be kept in the cache after it was last accessed."],
    arity = "1"
  )
  var jobCacheTimeoutDays = DefaultJobCacheTimeoutDays
    private set

  // endregion Job Configuration

  // region EDA Services

  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃  External EDA Service Connection Configuration                       ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  @Option(
    names = ["--eda-subsetting-host"],
    defaultValue = "\${env:EDA_SUBSETTING_HOST}",
    description = ["Hostname of the EDA Subsetting Service"],
    arity = "1",
    required = true
  )
  var edaSubsettingHost = UnconfiguredStringValue
    private set

  @Option(
    names = ["--eda-merge-host"],
    defaultValue = "\${env:EDA_MERGE_HOST}",
    description = ["Hostname of the EDA Merge Service"],
    arity = "1",
    required = true
  )
  var edaMergeHost = UnconfiguredStringValue
    private set

  @Option(
    names = ["--dataset-access-host"],
    defaultValue = "\${env:DATASET_ACCESS_HOST}",
    description = ["Hostname of the Dataset Access Service"],
    arity = "1",
    required = true
  )
  var datasetAccessHost = UnconfiguredStringValue
    private set

  // endregion EDA Services

  // region RServe

  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃  RServe Connection Configuration                                     ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  @Option(
    names = ["--rserve-host"],
    defaultValue = "\${env:RSERVE_HOST}",
    description = ["Hostname of an RServe instance."],
    arity = "1",
    required = true
  )
  var rServeHost = UnconfiguredStringValue
    private set

  // endregion RServe

  // region Admin

  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃  Administration Configuration                                        ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  @Option(
    names = ["--admin-auth-token"],
    defaultValue = "\${env:ADMIN_AUTH_TOKEN}",
    description = ["Special authorization token used to access admin endpoints."],
    arity = "1",
    required = true
  )
  @JvmStatic
  var adminAuthToken = UnconfiguredStringValue
    private set

  // endregion Admin

}
