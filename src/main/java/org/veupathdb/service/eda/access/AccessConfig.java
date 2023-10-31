package org.veupathdb.service.eda.access;

import org.jetbrains.annotations.Nullable;
import org.veupathdb.lib.container.jaxrs.config.Options;
import picocli.CommandLine;

import java.util.Optional;

public class AccessConfig extends Options {
  private static final String
    DEFAULT_REGISTRATION_PATH = "/app/user/registration",
    DEFAULT_APPLICATION_PATH  = "/app/study-access";

  private static final String UnconfiguredStringValue = "unconfigured";

  // Database Defaults
  private static final int DefaultQueueDBPort = 5432;
  private static final int DefaultQueueDBPoolSize = 10;

// Job Queue Defaults
  private static final int DefaultJobQueuePort = 5672;
  private static final String DefaultSlowQueueName = "slow-jobs";
  private static final int DefaultSlowQueueWorkers = 5;
  private static final String DefaultFastQueueName = "fast-jobs";
  private static final int DefaultFastQueueWorkers = 5;


// S3 Defaults
  private static final int DefaultS3Port     = 80;
  private static final boolean DefaultS3UseHttps = true;

// Job Cache Defaults
  private static final int DefaultJobCacheTimeoutDays = 30;

// RServe Defaults
  private static final int DefaultRServePort = 6311;


  @CommandLine.Option(
    names = "--enable-email",
    defaultValue = "${env:ENABLE_EMAIL}",
    arity = "1"
  )
  @SuppressWarnings("FieldMayBeFinal")
  private boolean enableEmail = true;

  @CommandLine.Option(
    names = "--smtp-host",
    defaultValue = "${env:SMTP_HOST}",
    required = true,
    arity = "1"
  )
  private String smtpHost;

  @CommandLine.Option(
    names = "--mail-debug",
    defaultValue = "${env:EMAIL_DEBUG}",
    arity = "1"
  )
  @SuppressWarnings("FieldMayBeFinal")
  private boolean emailDebug = false;

  @CommandLine.Option(
    names = "--support-email",
    defaultValue = "${env:SUPPORT_EMAIL}",
    required = true,
    arity = "1"
  )
  private String supportEmail;

  @CommandLine.Option(
    names = "--site-url",
    defaultValue = "${env:SITE_URL}",
    required = true,
    arity = "1"
  )
  private String siteUrl;

  @CommandLine.Option(
    names = "--registration-path",
    defaultValue = "${env:REGISTRATION_PATH}",
    arity = "1",
    description = "Path to the user registration client app component relative to $SITE_URL."
  )
  @SuppressWarnings("FieldMayBeFinal")
  private String registrationPath = DEFAULT_REGISTRATION_PATH;

  @CommandLine.Option(
    names = "--application-path",
    defaultValue = "${env:APP_PATH}",
    arity = "1",
    description = "Path to the client app component used to manage dataset access relative to $SITE_URL."
  )
  @SuppressWarnings("FieldMayBeFinal")
  private String applicationPath = DEFAULT_APPLICATION_PATH;
  
   /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃  Queue PostgreSQL                                                    ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  @CommandLine.Option(
      names = "--queue-db-name",
      defaultValue = "${env:QUEUE_DB_NAME}",
      description = "Queue database name",
      arity = "1",
      required = true
  )
  private String queueDBName = UnconfiguredStringValue;

  @CommandLine.Option(
      names = "--queue-db-host",
      defaultValue = "${env:QUEUE_DB_HOST}",
      description = "Queue database hostname",
      arity = "1",
      required = true
  )
  String queueDBHost = UnconfiguredStringValue;

  @CommandLine.Option(
      names = "--queue-db-port",
      defaultValue = "${env:QUEUE_DB_PORT}",
      description = "Queue database host port",
      arity = "1"
  )
  int queueDBPort = DefaultQueueDBPort;

  @CommandLine.Option(
      names = "--queue-db-username",
      defaultValue = "${env:QUEUE_DB_USERNAME}",
      description = "Queue database username",
      arity = "1",
      required = true
  )
  String queueDBUsername = UnconfiguredStringValue;

  @CommandLine.Option(
      names = "--queue-db-password",
      defaultValue = "${env:QUEUE_DB_PASSWORD}",
      description = "Queue database password",
      arity = "1",
      required = true
  )
  String queueDBPassword = UnconfiguredStringValue;

  @CommandLine.Option(
      names = "--queue-db-pool-size",
      defaultValue = "${env:QUEUE_DB_POOL_SIZE}",
      description = "Queue database pool size",
      arity = "1"
  )
  int queueDBPoolSize = DefaultQueueDBPoolSize;

  // endregion Postgres

  // region RabbitMQ

  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃  Queue RabbitMQ                                                      ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  @CommandLine.Option(
      names = "--job-queue-username",
      defaultValue = "${env:JOB_QUEUE_USERNAME}",
      description = "Username for the RabbitMQ instance",
      arity = "1",
      required = true
  )
  String jobQueueUsername;
  

  @CommandLine.Option(
      names = "--job-queue-password",
      defaultValue = "${env:JOB_QUEUE_PASSWORD}",
      description = "Password for the RabbitMQ instance",
      arity = "1",
      required = true
  )
  String jobQueuePassword;
  

  @CommandLine.Option(
      names = "--job-queue-host",
      defaultValue = "${env:JOB_QUEUE_HOST}",
      description = "Hostname for the RabbitMQ instance.",
      arity = "1",
      required = true
  )
  String jobQueueHost;
  

  @CommandLine.Option(
      names = "--job-queue-port",
      defaultValue = "${env:JOB_QUEUE_PORT}",
      description = "Host port for the RabbitMQ instance.",
      arity = "1"
  )
  int jobQueuePort;
  

  @CommandLine.Option(
      names = "--slow-queue-name",
      defaultValue = "${env:SLOW_QUEUE_NAME}",
      description = "Name of the slow jobs queue.",
      arity = "1"
  )
  String slowQueueName;
  

  @CommandLine.Option(
      names = "--slow-queue-workers",
      defaultValue = "${env:SLOW_QUEUE_WORKERS}",
      description = "Number of worker threads used by the slow job queue.",
      arity = "1"
  )
  int slowQueueWorkers;
  

  @CommandLine.Option(
      names = "--fast-queue-name",
      defaultValue = "${env:FAST_QUEUE_NAME}",
      description = "Name of the fast jobs queue.",
      arity = "1"
  )
  String fastQueueName = DefaultFastQueueName;
  

  @CommandLine.Option(
      names = "--fast-queue-workers",
      defaultValue = "${env:FAST_QUEUE_WORKERS}",
      description = "Number of worker threads used by the fast job queue.",
      arity = "1"
  )
  int fastQueueWorkers = DefaultFastQueueWorkers;
  

  // endregion RabbitMQ

  // region Minio (S3)

  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃  Queue S3 Instance                                                   ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  @CommandLine.Option(
      names = "--s3-host",
      defaultValue = "${env:S3_HOST}",
      description = "S3 instance hostname",
      arity = "1",
      required = true
  )
  String s3Host = UnconfiguredStringValue;
  

  @CommandLine.Option(
      names = "--s3-bucket",
      defaultValue = "${env:S3_BUCKET}",
      description = "S3 bucket name",
      arity = "1",
      required = true
  )
  String s3Bucket = UnconfiguredStringValue;
  

  @CommandLine.Option(
      names = "--s3-access-token",
      defaultValue = "${env:S3_ACCESS_TOKEN}",
      description = "S3 access token",
      arity = "1",
      required = true
  )
  String s3AccessToken = UnconfiguredStringValue;
  

  @CommandLine.Option(
      names = "--s3-secret-key",
      defaultValue = "${env:S3_SECRET_KEY}",
      description = "S3 secret key",
      arity = "1",
      required = true
  )
  String s3SecretKey = UnconfiguredStringValue;
  

  @CommandLine.Option(
      names = "--s3-port",
      defaultValue = "${env:S3_PORT}",
      description = "S3 host port",
      arity = "1"
  )
  int s3Port = DefaultS3Port;
  

  @CommandLine.Option(
      names = "--s3-use-https",
      defaultValue = "${env:S3_USE_HTTPS}",
      description = "Whether the platform should use HTTPS when connecting to S3",
      arity = "1"
  )
  boolean s3UseHttps = DefaultS3UseHttps;
  

  // endregion Minio (S3)

  // region Job Configuration

  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃  Queue Job Configuration                                             ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  @CommandLine.Option(
      names = "--job-cache-timeout-days",
      defaultValue = "${env:JOB_CACHE_TIMEOUT_DAYS}",
      description = "Number of days a job will be kept in the cache after it was last accessed.",
      arity = "1"
  )
  int jobCacheTimeoutDays = DefaultJobCacheTimeoutDays;
  

  // endregion Job Configuration

  // region EDA Services

  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃  External EDA Service Connection Configuration                       ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  @CommandLine.Option(
      names = "--eda-subsetting-host",
      defaultValue = "${env:EDA_SUBSETTING_HOST}",
      description = "Hostname of the EDA Subsetting Service",
      arity = "1",
      required = true
  )
  String edaSubsettingHost = UnconfiguredStringValue;

  @CommandLine.Option(
      names = "--eda-merge-host",
      defaultValue = "${env:EDA_MERGE_HOST}",
      description = "Hostname of the EDA Merge Service",
      arity = "1",
      required = true
  )
  String edaMergeHost = UnconfiguredStringValue;

  @CommandLine.Option(
      names = "--dataset-access-host",
      defaultValue = "${env:DATASET_ACCESS_HOST}",
      description = "Hostname of the Dataset Access Service",
      arity = "1",
      required = true
  )
  String datasetAccessHost = UnconfiguredStringValue;

  // endregion EDA Services

  // region RServe

  /*┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓*\
    ┃  RServe Connection Configuration                                     ┃
  \*┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛*/

  @CommandLine.Option(
      names = "--rserve-host",
      defaultValue = "${env:RSERVE_HOST}",
      description = "Hostname of an RServe instance.",
      arity = "1",
      required = true
  )
  String rServeHost = UnconfiguredStringValue;


  public boolean isEmailEnabled() {
    return enableEmail;
  }

  public String getSmtpHost() {
    return smtpHost;
  }

  public boolean isEmailDebug() {
    return emailDebug;
  }

  public String getSupportEmail() {
    return supportEmail;
  }

  public String getSiteUrl() {
    return siteUrl;
  }

  public String getRegistrationPath() {
    return registrationPath;
  }

  public String getApplicationPath() {
    return applicationPath;
  }

  public String getQueueDBName() {
    return queueDBName;
  }

  public String getQueueDBUsername() {
    return queueDBUsername;
  }

  public String getQueueDBPassword() {
    return queueDBPassword;
  }

  public String getQueueDBHost() {
    return queueDBHost;
  }

  public int getQueueDBPort() {
    return queueDBPort;
  }

  public int getQueueDBPoolSize() {
    return queueDBPoolSize;
  }

  public String getS3Host() {
    return s3Host;
  }

  public int getS3Port() {
    return s3Port;
  }

  public boolean getS3UseHttps() {
    return s3UseHttps;
  }

  public String getS3Bucket() {
    return s3Bucket;
  }

  public String getS3AccessToken() {
    return s3AccessToken;
  }

  public String getS3SecretKey() {
    return s3SecretKey;
  }

  public String getSlowQueueName() {
    return slowQueueName;
  }

  public String getJobQueueUsername() {
    return jobQueueUsername;
  }

  public String getJobQueuePassword() {
    return jobQueuePassword;
  }

  public String getJobQueueHost() {
    return jobQueueHost;
  }

  public int getJobQueuePort() {
    return jobQueuePort;
  }

  public int getSlowQueueWorkers() {
    return slowQueueWorkers;
  }

  public String getFastQueueName() {
    return fastQueueName;
  }

  public int getFastQueueWorkers() {
    return fastQueueWorkers;
  }

  public int getJobCacheTimeoutDays() {
    return jobCacheTimeoutDays;
  }

  public String getDatasetAccessHost() {
    return datasetAccessHost;
  }

  public String getEdaMergeHost() {
    return edaMergeHost;
  }

  public String getEdaSubsettingHost() {
    return edaSubsettingHost;
  }

  @Nullable
  public String getRServeHost() {
    return rServeHost;
  }
}
