package org.veupathdb.service.eda.ds.plugin.abundance;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.ListBuilder;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;

public class AbundanceScatterplotPlugin extends AbstractPlugin<AbundanceScatterplotPostRequest, ScatterplotWith1ComputeSpec, RankedAbundanceComputeConfig> {

  private static final Logger LOG = LogManager.getLogger(AbundanceScatterplotPlugin.class);
  
  @Override
  public String getDisplayName() {
    return "Scatter plot";
  }

  @Override
  public String getDescription() {
    return "Visualize the relationship between a continuous variable and abundance";
  }

  @Override
  public List<String> getProjects() {
    return List.of(AppsMetadata.MICROBIOME_PROJECT);
  }

  @Override
  protected Class<AbundanceScatterplotPostRequest> getVisualizationRequestClass() {
    return AbundanceScatterplotPostRequest.class;
  }

  @Override
  protected Class<ScatterplotWith1ComputeSpec> getVisualizationSpecClass() {
    return ScatterplotWith1ComputeSpec.class;
  }

  @Override
  protected Class<RankedAbundanceComputeConfig> getComputeConfigClass() {
    return RankedAbundanceComputeConfig.class;
  }

  @Override
  protected boolean includeComputedVarsInStream() {
    return true;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("xAxisVariable"), List.of("facetVariable"))
      .pattern()
        .element("xAxisVariable")
          .types(APIVariableType.NUMBER, APIVariableType.DATE, APIVariableType.INTEGER)
          .description("Variable must be a number or date and be of the same or a parent entity as the Y-axis variable.")
        .element("facetVariable")
          .required(false)
          .maxVars(2)
          .maxValues(7)
          .description("Variable(s) must have7 or fewer unique values and be of the same or a parent entity as the Overlay variable.")
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(ScatterplotWith1ComputeSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("facetVariable", pluginSpec.getFacetVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(ScatterplotWith1ComputeSpec pluginSpec) {
    List<StreamSpec> requestedStreamsList = ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
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
    varMap.put("facet1", util.getVariableSpecFromList(spec.getFacetVariable(), 0));
    varMap.put("facet2", util.getVariableSpecFromList(spec.getFacetVariable(), 1));
    String valueSpec = spec.getValueSpec().getValue();
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "noVariables";
    String deprecatedShowMissingness = showMissingness.equals("FALSE") ? "noVariables" : showMissingness.equals("TRUE") ? "strataVariables" : showMissingness;

    ComputedVariableMetadata metadata = getComputedVariableMetadata();
    metadata.getVariables().get(0).setPlotReference(PlotReferenceValue.OVERLAY);

    List<VariableSpec> inputVarSpecs = new ArrayList<VariableSpec>();
    inputVarSpecs.addAll(metadata.getVariables().stream()
        .filter(var -> var.getPlotReference().getValue().equals("overlay"))
        .findFirst().orElseThrow().getMembers().subList(0,8));
    System.out.println("number collection members added to inputs: " + inputVarSpecs.size());
    inputVarSpecs.add(spec.getXAxisVariable());
    inputVarSpecs.add(util.getVariableSpecFromList(spec.getFacetVariable(), 0));
    inputVarSpecs.add(util.getVariableSpecFromList(spec.getFacetVariable(), 1));

    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME, inputVarSpecs));
      connection.voidEval(getVoidEvalVariableMetadataList(varMap));
      connection.voidEval(getVoidEvalComputedVariableMetadataList(metadata));
      connection.voidEval("variables <- veupathUtils::merge(variables, computedVariables)");
      connection.voidEval("overlayVarMetadata <- veupathUtils::findVariableMetadataFromPlotRef(variables, 'overlay')");
      connection.voidEval("overlayVarMetadata@members <- overlayVarMetadata@members[1:8]");
      connection.voidEval("overlayVarIndex <- veupathUtils::findIndexFromPlotRef(variables, 'overlay')");
      connection.voidEval("variables[[overlayVarIndex]] <- overlayVarMetadata");

      String command = "plot.data::scattergl(" + DEFAULT_SINGLE_STREAM_NAME + ", variables, '" +
          valueSpec + "', '" + 
          deprecatedShowMissingness + "')";
      RServeClient.streamResult(connection, command, out);
    }); 
  }
}
