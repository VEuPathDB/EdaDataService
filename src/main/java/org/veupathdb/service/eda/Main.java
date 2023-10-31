package org.veupathdb.service.eda;

import org.gusdb.fgputil.db.slowquery.QueryLogConfig;
import org.gusdb.fgputil.db.slowquery.QueryLogger;
import org.veupathdb.lib.compute.platform.AsyncPlatform;
import org.veupathdb.lib.compute.platform.config.AsyncDBConfig;
import org.veupathdb.lib.compute.platform.config.AsyncJobConfig;
import org.veupathdb.lib.compute.platform.config.AsyncPlatformConfig;
import org.veupathdb.lib.compute.platform.config.AsyncQueueConfig;
import org.veupathdb.lib.compute.platform.config.AsyncS3Config;
import org.veupathdb.lib.container.jaxrs.config.Options;
import org.veupathdb.lib.container.jaxrs.server.ContainerResources;
import org.veupathdb.lib.container.jaxrs.server.Server;
import org.veupathdb.service.eda.access.AccessConfig;
import org.veupathdb.service.eda.compute.exec.PluginExecutor;

public class Main extends Server {
  public static final AccessConfig config = new AccessConfig();

  public static void main(String[] args) {
    new Main().start(args);
  }

  public Main() {
    QueryLogger.initialize(new QLF(){});
  }

  @Override
  protected ContainerResources newResourceConfig(Options options) {
    //    out.property("jersey.config.server.tracing.type", "ALL")
//       .property("jersey.config.server.tracing.threshold", "VERBOSE");
    return new Resources(config);
  }

  @Override
  protected Options newOptions() {
    return config;
  }

  @Override
  protected void onShutdown() {
    Resources.getDeserializerThreadPool().shutdown();
    Resources.getFileChannelThreadPool().shutdown();
    Resources.getMetadataCache().shutdown();
  }

  @Override
  protected void postCliParse(Options opts) {
    initAsyncPlatform();
  }

  public static class QLF implements QueryLogConfig {
    public double getBaseline() {
      return 0.05D;
    }

    public double getSlow() {
      return 1.0D;
    }

    public boolean isIgnoredSlow(String sql) {
      return false;
    }

    public boolean isIgnoredBaseline(String sql) {
      return false;
    }
  }

  private void initAsyncPlatform() {
    AsyncDBConfig dbConfig = new AsyncDBConfig(
        config.getQueueDBName(),
        config.getQueueDBUsername(),
        config.getQueueDBPassword(),
        config.getQueueDBHost(),
        config.getQueueDBPort(),
        config.getQueueDBPoolSize()
    );
    AsyncS3Config s3Config = new AsyncS3Config(
        config.getS3Host(),
        config.getS3Port(),
        config.getS3UseHttps(),
        config.getS3Bucket(),
        config.getS3AccessToken(),
        config.getS3SecretKey(),
        "/"
    );
    AsyncJobConfig jobConfig = new AsyncJobConfig(
        (ctx) -> new PluginExecutor(),
        config.getJobCacheTimeoutDays()
    );
    AsyncPlatformConfig asyncConfig = AsyncPlatformConfig.builder()
        .dbConfig(dbConfig)
        .s3Config(s3Config)
        .jobConfig(jobConfig)
        .addQueue(new AsyncQueueConfig(
            config.getSlowQueueName(),
            config.getJobQueueUsername(),
            config.getJobQueuePassword(),
            config.getJobQueueHost(),
            config.getJobQueuePort(),
            config.getSlowQueueWorkers()))
        .addQueue(new AsyncQueueConfig(
            config.getFastQueueName(),
            config.getJobQueueUsername(),
            config.getJobQueuePassword(),
            config.getJobQueueHost(),
            config.getJobQueuePort(),
            config.getFastQueueWorkers()))
        .build();
    AsyncPlatform.init(asyncConfig);
  }
}
