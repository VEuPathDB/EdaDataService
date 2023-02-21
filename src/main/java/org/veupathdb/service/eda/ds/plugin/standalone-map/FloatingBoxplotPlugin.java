package org.veupathdb.service.eda.ds.plugin.pass;

import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.common.plugin.util.RFileSetProcessor;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.generated.model.*;

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

public class FloatingBoxplotPlugin extends AbstractEmptyComputePlugin<FloatingBoxplotPostRequest, FloatingBoxplotSpec> {

  @Override
  public String getDisplayName() {
    return "Box plot";
  }

  @Override
  public String getDescription() {
    return "Visualize summary values for a continuous variable";
  }

  @Override
  public List<String> getProjects() {
    return List.of(VECTORBASE_PROJECT);
  }

  @Override
  protected Class<FloatingBoxplotPostRequest> getVisualizationRequestClass() {
    return FloatingBoxplotPostRequest.class;
  }

  @Override
  protected Class<FloatingBoxplotSpec> getVisualizationSpecClass() {
    return FloatingBoxplotSpec.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("yAxisVariable"), List.of("xAxisVariable"), List.of("overlayVariable"))
      .pattern()
        .element("yAxisVariable")
          .types(APIVariableType.NUMBER, APIVariableType.INTEGER)
          .description("Variable must be a number and be of the same or a child entity as the X-axis variable.")
        .element("xAxisVariable")
          .maxValues(10)
          .description("Variable must have 10 or fewer unique values and be the same or a child entity as the variable the map markers are painted with.")
        .element("overlayVariable")
          .required(false)
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(FloatingBoxplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("yAxisVariable", pluginSpec.getYAxisVariable())
      .var("overlayVariable", pluginSpec.getOverlayVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(FloatingBoxplotSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getYAxisVariable())
        .addVar(pluginSpec.getOverlayVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    PluginUtil util = getUtil();
    FloatingBoxplotSpec spec = getPluginSpec();
    Map<String, VariableSpec> varMap = new HashMap<String, VariableSpec>();
    varMap.put("xAxis", spec.getXAxisVariable());
    varMap.put("yAxis", spec.getYAxisVariable());
    varMap.put("overlay", spec.getOverlayVariable());
    
    List<String> nonStrataVarColNames = new ArrayList<String>();
    nonStrataVarColNames.add(util.toColNameOrEmpty(spec.getXAxisVariable()));
    nonStrataVarColNames.add(util.toColNameOrEmpty(spec.getYAxisVariable()));

    RFileSetProcessor filesProcessor = new RFileSetProcessor(dataStreams)
      .add(DEFAULT_SINGLE_STREAM_NAME, 
        spec.getMaxAllowedDataPoints(), 
        "noVariables", 
        nonStrataVarColNames, 
        (name, conn) ->
        conn.voidEval(util.getVoidEvalFreadCommand(name,
          spec.getXAxisVariable(),
          spec.getYAxisVariable(),
          spec.getOverlayVariable()))
      );

    useRConnectionWithProcessedRemoteFiles(Resources.RSERVE_URL, filesProcessor, connection -> {
      connection.voidEval(getVoidEvalVariableMetadataList(varMap));
      String cmd =
          "plot.data::box(data=" + DEFAULT_SINGLE_STREAM_NAME + ", variables=variables, " +
              "points='outliers', " +
              "mean=TRUE, " +
              "computeStats=FALSE, " +
              "sampleSizes=FALSE, " +
              "completeCases=FALSE, '" +
              deprecatedShowMissingness + "')";
      streamResult(connection, cmd, out);
    });
  }
}
