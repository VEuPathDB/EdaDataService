package org.veupathdb.service.eda.ds.plugin.pass;

import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.generated.model.FloatingMosaicPostRequest;
import org.veupathdb.service.eda.generated.model.FloatingMosaicSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class FloatingContTablePlugin extends AbstractEmptyComputePlugin<FloatingMosaicPostRequest, FloatingMosaicSpec> {

  @Override
  public String getDisplayName() {
    return "Mosaic plot";
  }

  @Override
  public String getDescription() {
    return "Visualize the frequency distribution and chi-squared test results for two categorical variables";
  }

  @Override
  public List<String> getProjects() {
    return List.of(VECTORBASE_PROJECT);
  }
  
  @Override
  protected Class<FloatingMosaicSpec> getVisualizationSpecClass() {
    return FloatingMosaicSpec.class;
  }

  @Override
  protected Class<FloatingMosaicPostRequest> getVisualizationRequestClass() {
    return FloatingMosaicPostRequest.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("overlayVariable", "xAxisVariable"))
      .pattern()
        .element("overlayVariable") // required here, since its functionally the yaxis
        .element("xAxisVariable")
          .maxValues(10)
          .description("Variable must have 10 or fewer unique values and be from the same branch of the dataset diagram as the variable the map markers are painted with.")
      .done();
  }

  @Override
  protected void validateVisualizationSpec(FloatingMosaicSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("overlayVariable", pluginSpec.getOverlayVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(FloatingMosaicSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getOverlayVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    PluginUtil util = getUtil();
    FloatingMosaicSpec spec = getPluginSpec();
    Map<String, VariableSpec> varMap = new HashMap<String, VariableSpec>();
    varMap.put("xAxis", spec.getXAxisVariable());
    varMap.put("yAxis", spec.getOverlayVariable());
    
    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME,
          spec.getXAxisVariable(),
          spec.getOverlayVariable()));
      connection.voidEval(getVoidEvalVariableMetadataList(varMap));
      String cmd = "plot.data::mosaic(data=" + DEFAULT_SINGLE_STREAM_NAME + ", variables=variables, " + 
                                     "statistic='chiSq', " + 
                                     "columnReferenceValue=NA_character_, " + 
                                     "rowReferenceValue=NA_character_, "+
                                     "sampleSizes=FALSE, " +
                                     "completeCases=FALSE, 'noVariables')";
      streamResult(connection, cmd, out);
    });
  }
}
