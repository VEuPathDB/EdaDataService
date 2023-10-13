package org.veupathdb.service.eda.compute.plugins.differentialabundance;

import org.jetbrains.annotations.NotNull;
import org.veupathdb.service.eda.compute.plugins.PluginContext;
import org.veupathdb.service.eda.compute.plugins.PluginProvider;
import org.veupathdb.service.eda.compute.plugins.PluginQueue;
import org.veupathdb.service.eda.generated.model.DifferentialAbundanceComputeConfig;
import org.veupathdb.service.eda.generated.model.DifferentialAbundancePluginRequest;
import org.veupathdb.service.eda.generated.model.DifferentialAbundancePluginRequestImpl;

public class DifferentialAbundancePluginProvider implements PluginProvider<DifferentialAbundancePluginRequest, DifferentialAbundanceComputeConfig> {
  @NotNull
  @Override
  public String getUrlSegment() {
    return "differentialabundance";
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "Differential Abundance Plugin";
  }

  @NotNull
  @Override
  public PluginQueue getTargetQueue() {
    return PluginQueue.Slow;
  }

  @NotNull
  @Override
  public Class<? extends DifferentialAbundancePluginRequest> getRequestClass() {
    return DifferentialAbundancePluginRequestImpl.class;
  }

  @NotNull
  @Override
  public DifferentialAbundancePlugin createPlugin(@NotNull PluginContext<DifferentialAbundancePluginRequest, DifferentialAbundanceComputeConfig> context) {
    return new DifferentialAbundancePlugin(context);
  }
}
