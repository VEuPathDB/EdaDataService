package org.veupathdb.service.eda.ds.plugin.sample;

import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.ExampleComputeConfig;
import org.veupathdb.service.eda.generated.model.ExampleComputeVizPostRequest;
import org.veupathdb.service.eda.generated.model.ExampleComputeVizSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class ExampleComputeVizPlugin extends AbstractPlugin<ExampleComputeVizPostRequest, ExampleComputeVizSpec, ExampleComputeConfig> {

  @Override
  protected Class<ExampleComputeConfig> getComputeConfigClass() {
    return ExampleComputeConfig.class;
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

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    // TODO
  }

}
