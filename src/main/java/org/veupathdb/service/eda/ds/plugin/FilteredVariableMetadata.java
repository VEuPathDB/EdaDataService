package org.veupathdb.service.eda.ds.plugin;

import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.common.plugin.util.RFileSetProcessor;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.BoxplotPostRequest;
import org.veupathdb.service.eda.generated.model.ContinuousVariableMetadataSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithProcessedRemoteFiles;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.CLINEPI_PROJECT;

public class FilteredVariableMetadataPlugin extends AbstractEmptyComputePlugin<ContinuousVariableMetadataPostRequest, ContinuousVariableMetadataSpec> {
  
  @Override
  protected Class<ContinuousVariableMetadataSpec> getVisualizationSpecClass() {
    return ContinuousVariableMetadataSpec.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("variable"))
      .pattern()
        .element("variable")
          .shapes(APIVariableDataShape.CONTINUOUS)
          .description("Variable must be continuous.")
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(ContinuousVariableMetadataSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .var("variable", pluginSpec.getVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(ContinuousVariableMetadataSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getVariable().getEntity())
        .addVar(pluginSpec.getVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    PluginUtil util = getUtil();
    ContinuousVariableMetadataSpec spec = getPluginSpec();

    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      
    });
  }
}
