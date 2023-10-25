package org.veupathdb.service.eda;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.gusdb.fgputil.db.slowquery.QueryLogConfig;
import org.gusdb.fgputil.db.slowquery.QueryLogger;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.runtime.ProjectSpecificProperties;
import org.veupathdb.lib.container.jaxrs.config.Options;
import org.veupathdb.lib.container.jaxrs.server.ContainerResources;
import org.veupathdb.lib.container.jaxrs.server.Server;
import org.veupathdb.lib.container.jaxrs.utils.db.DbManager;
import org.veupathdb.service.eda.access.AccessConfig;
import org.veupathdb.service.eda.access.repo.ApprovalStatusRepo;
import org.veupathdb.service.eda.access.repo.RestrictionLevelRepo;

import static org.gusdb.fgputil.runtime.ProjectSpecificProperties.PropertySpec.required;

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

}
