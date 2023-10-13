package org.veupathdb.service.eda.compute.plugins.rankedabundance;

import org.jetbrains.annotations.NotNull;
import org.veupathdb.service.eda.compute.plugins.PluginContext;
import org.veupathdb.service.eda.compute.plugins.PluginProvider;
import org.veupathdb.service.eda.compute.plugins.PluginQueue;
import org.veupathdb.service.eda.generated.model.RankedAbundanceComputeConfig;
import org.veupathdb.service.eda.generated.model.RankedAbundancePluginRequest;
import org.veupathdb.service.eda.generated.model.RankedAbundancePluginRequestImpl;

public class RankedAbundancePluginProvider implements PluginProvider<RankedAbundancePluginRequest, RankedAbundanceComputeConfig> {
  @NotNull
  @Override
  public String getUrlSegment() {
    return "rankedabundance";
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "Ranked Abundance Plugin";
  }

  @NotNull
  @Override
  public PluginQueue getTargetQueue() {
    return PluginQueue.Fast;
  }

  @NotNull
  @Override
  public Class<? extends RankedAbundancePluginRequest> getRequestClass() {
    return RankedAbundancePluginRequestImpl.class;
  }

  @NotNull
  @Override
  public RankedAbundancePlugin createPlugin(@NotNull PluginContext<RankedAbundancePluginRequest, RankedAbundanceComputeConfig> context) {
    return new RankedAbundancePlugin(context);
  }
}
