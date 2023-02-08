package org.veupathdb.service.eda.ds.plugin.alphadiv;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.common.plugin.util.RServeClient;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;

public class AlphaDivScatterplotPlugin extends AbstractPlugin<AlphaDivScatterplotPostRequest, ScatterplotWith1ComputeSpec, AlphaDivComputeConfig> {

  private static final Logger LOG = LogManager.getLogger(AlphaDivScatterplotPlugin.class);
  
  @Override
  public String getDisplayName() {
    return "Scatter plot";
  }

  @Override
  public String getDescription() {
    return "Visualize the relationship between a continuous variable and alpha diversity";
  }

  @Override
  public List<String> getProjects() {
    return List.of(AppsMetadata.MICROBIOME_PROJECT);
  }

  @Override
  protected Class<AlphaDivScatterplotPostRequest> getVisualizationRequestClass() {
    return AlphaDivScatterplotPostRequest.class;
  }
  
  @Override
  protected Class<ScatterplotWith1ComputeSpec> getVisualizationSpecClass() {
    return ScatterplotWith1ComputeSpec.class;
  }

  @Override
  protected Class<AlphaDivComputeConfig> getComputeConfigClass() {
    return AlphaDivComputeConfig.class;
  }

  @Override
  protected boolean includeComputedVarsInStream() {
    return true;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("xAxisVariable"), List.of("overlayVariable", "facetVariable"))
      .pattern()
        .element("xAxisVariable")
          .types(APIVariableType.NUMBER, APIVariableType.DATE, APIVariableType.INTEGER)
          .description("Variable must be a number or date and be of the same or a parent entity as the Y-axis variable.")
        .element("overlayVariable")
          .required(false)
          .maxValues(8)
          .description("Variable must be of the same or a parent entity as the X-axis variable.")
        .element("facetVariable")
          .required(false)
          .maxVars(2)
          .maxValues(7)
          .description("Variable(s) must have 7 or fewer unique values and be of the same or a parent entity as the Overlay variable.")
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(ScatterplotWith1ComputeSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("overlayVariable", pluginSpec.getOverlayVariable())
      .var("facetVariable", pluginSpec.getFacetVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(ScatterplotWith1ComputeSpec pluginSpec) {
    List<StreamSpec> requestedStreamsList = List.of(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getOverlayVariable())
        .addVars(pluginSpec.getFacetVariable())
        .setIncludeComputedVars(true)
    );
    return requestedStreamsList;
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    ScatterplotWith1ComputeSpec spec = getPluginSpec();
    PluginUtil util = getUtil();
    Map<String, VariableSpec> varMap = new HashMap<>();
    varMap.put("xAxis", spec.getXAxisVariable());
    varMap.put("overlay", spec.getOverlayVariable());
    varMap.put("facet1", util.getVariableSpecFromList(spec.getFacetVariable(), 0));
    varMap.put("facet2", util.getVariableSpecFromList(spec.getFacetVariable(), 1));
    String valueSpec = spec.getValueSpec().getValue();
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "noVariables";
    String deprecatedShowMissingness = showMissingness.equals("FALSE") ? "noVariables" : showMissingness.equals("TRUE") ? "strataVariables" : showMissingness;
   
    ComputedVariableMetadata metadata = getComputedVariableMetadata();
    VariableSpec computedVarSpec = metadata.getVariables().stream()
        .filter(var -> var.getPlotReference().getValue().equals("yAxis"))
        .findFirst().orElseThrow().getVariableSpec();

    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME,
          computedVarSpec,
          spec.getXAxisVariable(),
          spec.getOverlayVariable(),
          util.getVariableSpecFromList(spec.getFacetVariable(), 0),
          util.getVariableSpecFromList(spec.getFacetVariable(), 1)));
      
      connection.voidEval(getVoidEvalVariableMetadataList(varMap));
      connection.voidEval(getVoidEvalComputedVariableMetadataList(metadata));
      connection.voidEval("variables <- veupathUtils::merge(variables, computedVariables)");
      
      String command = "plot.data::scattergl(" + DEFAULT_SINGLE_STREAM_NAME + ", variables, '" + valueSpec + "', '" + deprecatedShowMissingness + "')";
      RServeClient.streamResult(connection, command, out);
    }); 
  }
}
