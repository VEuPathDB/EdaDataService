package org.veupathdb.service.eda.ds.plugin.alphadiv;

import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.common.plugin.util.RServeClient;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.core.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;

public class AlphaDivBoxplotPlugin extends AbstractPlugin<AlphaDivBoxplotPostRequest, BoxplotWith1ComputeSpec, AlphaDivComputeConfig> {

  @Override
  public String getDisplayName() {
    return "Box plot";
  }

  @Override
  public String getDescription() {
    return "Visualize alpha diversity summary values";
  }

  @Override
  public List<String> getProjects() {
    return List.of(AppsMetadata.MICROBIOME_PROJECT);
  }

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new ClassGroup(AlphaDivBoxplotPostRequest.class, BoxplotWith1ComputeSpec.class, AlphaDivComputeConfig.class);
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
    .dependencyOrder(List.of("yAxisVariable"), List.of("xAxisVariable"), List.of("overlayVariable", "facetVariable"))
      .pattern()
        .element("xAxisVariable")
          .maxValues(10)
          .description("Variable must have 10 or fewer unique values and be the same or a parent entity of the Y-axis variable.") // Of taxa entity?
        .element("overlayVariable")
          .required(false)
          .maxValues(8)
          .description("Variable must have 8 or fewer unique values and be the same or a parent entity of the X-axis variable.")
        .element("facetVariable")
          .required(false)
          .maxVars(2)
          .maxValues(7)
          .description("Variable(s) must have 7 or fewer unique values and be of the same or a parent entity of the Overlay variable.")
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(BoxplotWith1ComputeSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("overlayVariable", pluginSpec.getOverlayVariable())
      .var("facetVariable", pluginSpec.getFacetVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(BoxplotWith1ComputeSpec pluginSpec) {
    return List.of(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getOverlayVariable())
        .addVars(pluginSpec.getFacetVariable())
        .setIncludeComputedVars(true)
    );
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    BoxplotWith1ComputeSpec spec = getPluginSpec();
    PluginUtil util = getUtil();
    Map<String, VariableSpec> varMap = new HashMap<>();
    varMap.put("xAxis", spec.getXAxisVariable());
    varMap.put("overlay", spec.getOverlayVariable());
    varMap.put("facet1", util.getVariableSpecFromList(spec.getFacetVariable(), 0));
    varMap.put("facet2", util.getVariableSpecFromList(spec.getFacetVariable(), 1));
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "noVariables";
    String deprecatedShowMissingness = showMissingness.equals("FALSE") ? "noVariables" : showMissingness.equals("TRUE") ? "strataVariables" : showMissingness;
    String computeStats = spec.getComputeStats() != null ? spec.getComputeStats().getValue() : "TRUE";
    String showMean = spec.getMean() != null ? spec.getMean().getValue() : "FALSE";

    // get metadata to find computed column
    ComputedVariableMetadata metadata = getComputedVariableMetadata();
    // TODO will probably do this a lot, make helper to find varspec from plot ref?
    VariableSpec computedVarSpec = metadata.getVariables().stream()
        .filter(var -> var.getPlotReference().getValue().equals("yAxis"))
        .findFirst().orElseThrow().getVariableSpec();
    
    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      // TODO this needs to add the computed vars now as well
      connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME,
          computedVarSpec,
          spec.getXAxisVariable(),
          spec.getOverlayVariable(),
          util.getVariableSpecFromList(spec.getFacetVariable(), 0),
          util.getVariableSpecFromList(spec.getFacetVariable(), 1)));
      
      connection.voidEval(getVoidEvalVariableMetadataList(varMap));
      connection.voidEval(getVoidEvalComputedVariableMetadataList(metadata));
      connection.voidEval("variables <- veupathUtils::merge(variables, computedVariables)");

      String command = "plot.data::box(" + DEFAULT_SINGLE_STREAM_NAME + ", variables, '" +
          spec.getPoints().getValue() + "', " +
          showMean + ", " + 
          computeStats + ", NULL, TRUE, TRUE, '" + 
          deprecatedShowMissingness +"')";
      RServeClient.streamResult(connection, command, out);
    });
  }
}
