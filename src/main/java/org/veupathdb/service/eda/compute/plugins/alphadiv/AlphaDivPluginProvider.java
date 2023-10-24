package org.veupathdb.service.eda.compute.plugins.alphadiv;

import org.jetbrains.annotations.NotNull;
import org.veupathdb.service.eda.compute.plugins.PluginContext;
import org.veupathdb.service.eda.compute.plugins.PluginProvider;
import org.veupathdb.service.eda.compute.plugins.PluginQueue;
import org.veupathdb.service.eda.generated.model.AlphaDivComputeConfig;
import org.veupathdb.service.eda.generated.model.AlphaDivPluginRequest;
import org.veupathdb.service.eda.generated.model.AlphaDivPluginRequestImpl;

public class AlphaDivPluginProvider implements PluginProvider<AlphaDivPluginRequest, AlphaDivComputeConfig> {

  @NotNull
  @Override
  public String getUrlSegment() {
    return "alphadiv";
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "Alpha Diversity Plugin";
  }

  @NotNull
  @Override
  public PluginQueue getTargetQueue() {
    return PluginQueue.Fast;
  }

  @NotNull
  @Override
  public Class<? extends AlphaDivPluginRequest> getRequestClass() {
    return AlphaDivPluginRequestImpl.class;
  }

  @NotNull
  @Override
  public AlphaDivPlugin createPlugin(@NotNull PluginContext<AlphaDivPluginRequest, AlphaDivComputeConfig> context) {
    return new AlphaDivPlugin(context);
  }
}
