package org.veupathdb.service.eda.ds.plugin.pass;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class FloatingScatterplotPlugin extends AbstractEmptyComputePlugin<FloatingScatterplotPostRequest, FloatingScatterplotSpec> {

  private static final Logger LOG = LogManager.getLogger(FloatingScatterplotPlugin.class);
  
  @Override
  public String getDisplayName() {
    return "Scatter plot";
  }

  @Override
  public String getDescription() {
    return "Visualize the relationship between two continuous variables";
  }

  @Override
  public List<String> getProjects() {
    return List.of(VECTORBASE_PROJECT);
  }

  @Override
  protected Class<FloatingScatterplotPostRequest> getVisualizationRequestClass() {
    return FloatingScatterplotPostRequest.class;
  }

  @Override
  protected Class<FloatingScatterplotSpec> getVisualizationSpecClass() {
    return FloatingScatterplotSpec.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("yAxisVariable", "xAxisVariable"), List.of("overlayVariable"))
      .pattern()
        .element("yAxisVariable")
          .types(APIVariableType.NUMBER, APIVariableType.DATE, APIVariableType.INTEGER) 
          .description("Variable must be a number or date and be of the same or a child entity as the X-axis variable.")
        .element("xAxisVariable")
          .types(APIVariableType.NUMBER, APIVariableType.DATE, APIVariableType.INTEGER)
          .description("Variable must be a number or date and be the same or a child entity as the variable the map markers are painted with.")
        .element("overlayVariable")
          .required(false)
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(FloatingScatterplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("yAxisVariable", pluginSpec.getYAxisVariable())
      .var("overlayVariable", pluginSpec.getOverlayVariable()));
    if (pluginSpec.getMaxAllowedDataPoints() != null && pluginSpec.getMaxAllowedDataPoints() <= 0) {
      throw new ValidationException("maxAllowedDataPoints must be a positive integer");
    }
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(FloatingScatterplotSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getYAxisVariable())
        .addVar(pluginSpec.getOverlayVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    PluginUtil util = getUtil();
    FloatingScatterplotSpec spec = getPluginSpec();
    Map<String, VariableSpec> varMap = new HashMap<String, VariableSpec>();
    varMap.put("xAxis", spec.getXAxisVariable());
    varMap.put("yAxis", spec.getYAxisVariable());
    varMap.put("overlay", spec.getOverlayVariable());
    String valueSpec = spec.getValueSpec().getValue();
    String yVarType = util.getVariableType(spec.getYAxisVariable());
    
    if (yVarType.equals("DATE") && !valueSpec.equals("raw")) {
      LOG.error("Cannot calculate trend lines for y-axis date variables. The `valueSpec` property must be set to `raw`.");
    }
    
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
          "plot.data::scattergl(data=" + DEFAULT_SINGLE_STREAM_NAME + ", variables=variables, '" + 
              "value=" + valueSpec + "', '" + 
              "sampleSizes=FALSE, " +
              "completeCases=FALSE, 'noVariables')";
      streamResult(connection, cmd, out);
    }); 
  }
}
