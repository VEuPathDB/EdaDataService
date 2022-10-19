package org.veupathdb.service.eda.ds.plugin.sample;

import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.ds.plugin.AbstractPluginWithCompute;
import org.veupathdb.service.eda.generated.model.ExampleComputeVizPostRequest;
import org.veupathdb.service.eda.generated.model.ExampleComputeVizSpec;
import org.veupathdb.service.eda.generated.model.ExamplePluginConfig;
import org.veupathdb.service.eda.generated.model.ExamplePluginRequest;

import java.util.List;

public class ExampleComputeVizPlugin extends AbstractPluginWithCompute<ExampleComputeVizPostRequest, ExampleComputeVizSpec, ExamplePluginConfig>  {

  @Override
  protected Class<ExamplePluginConfig> getComputeSpecClass() {
    return ExamplePluginConfig.class;
  }

  @Override
  protected Class<ExampleComputeVizSpec> getVisualizationSpecClass() {
    return ExampleComputeVizSpec.class;
  }

  @Override
  protected boolean includeComputedVarsInStream() {
    // this visualization uses a compute plugin that computes a variable (not just statistics)
    return true;
  }

  @Override
  protected void validateVisualizationSpec(ExampleComputeVizSpec pluginSpec) throws ValidationException {
    // TODO
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(ExampleComputeVizSpec pluginSpec) {
    return List.of(new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getEntityId())
        .addVar(pluginSpec.getPrefixVar())
        .setIncludeComputedVars(true));
  }


}
