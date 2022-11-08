package org.veupathdb.service.eda.ds.plugin.abundance;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.plugin.AbstractPluginWithCompute;
import org.veupathdb.service.eda.common.plugin.util.RServeClient;
import org.veupathdb.service.eda.generated.model.AbundanceBoxplotPostRequest;
import org.veupathdb.service.eda.generated.model.BoxplotWith1ComputeSpec;
import org.veupathdb.service.eda.generated.model.AbundanceComputeConfig;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.common.plugin.util.PluginUtil.singleQuote;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;

public class AbundanceBoxplotPlugin extends AbstractPluginWithCompute<AbundanceBoxplotPostRequest, BoxplotWith1ComputeSpec, AbundanceComputeConfig> {

  @Override
  public String getDisplayName() {
    return "Box plot";
  }

  @Override
  public String getDescription() {
    return "Visualize ranked abundance summary values";
  }

  @Override
  public List<String> getProjects() {
    return List.of(AppsMetadata.MICROBIOME_PROJECT);
  }
  
  @Override
  protected Class<BoxplotWith1ComputeSpec> getVisualizationSpecClass() {
    return BoxplotWith1ComputeSpec.class;
  }

  @Override
  protected Class<AbundanceComputeConfig> getComputeSpecClass() {
    return AbundanceComputeConfig.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("overlayVariable", "facetVariable"))
      .pattern()
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
      .var("overlayVariable", pluginSpec.getOverlayVariable())
      .var("facetVariable", pluginSpec.getFacetVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(BoxplotWith1ComputeSpec pluginSpec, AbundanceComputeConfig computeConfig) {
    List<StreamSpec> requestedStreamsList = ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getOverlayVariable())
        .addVars(pluginSpec.getFacetVariable())
      );
    requestedStreamsList.add(
      new StreamSpec(COMPUTE_STREAM_NAME, computeConfig.getCollectionVariable().getEntityId())
        .addVars(getUtil().getChildrenVariables(computeConfig.getCollectionVariable())
      ));
    
    return requestedStreamsList;
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    AbundanceComputeConfig computeConfig = getComputeConfig();
    BoxplotWith1ComputeSpec spec = getPluginSpec();
    PluginUtil util = getUtil();
    Map<String, VariableSpec> varMap = new HashMap<>();
    varMap.put("overlay", spec.getOverlayVariable());
    varMap.put("facet1", util.getVariableSpecFromList(spec.getFacetVariable(), 0));
    varMap.put("facet2", util.getVariableSpecFromList(spec.getFacetVariable(), 1));
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "noVariables";
    String deprecatedShowMissingness = showMissingness.equals("FALSE") ? "noVariables" : showMissingness.equals("TRUE") ? "strataVariables" : showMissingness;
    String computeStats = spec.getComputeStats() != null ? spec.getComputeStats().getValue() : "TRUE";
    String showMean = spec.getMean() != null ? spec.getMean().getValue() : "FALSE";
    String method = computeConfig.getRankingMethod().getValue();
    VariableDef computeEntityIdVarSpec = util.getEntityIdVarSpec(computeConfig.getCollectionVariable().getEntityId());
    String computeEntityIdColName = util.toColNameOrEmpty(computeEntityIdVarSpec);

    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      List<VariableSpec> computeInputVars = ListBuilder.asList(computeEntityIdVarSpec);
      computeInputVars.addAll(util.getChildrenVariables(computeConfig.getCollectionVariable()));
      connection.voidEval(util.getVoidEvalFreadCommand(COMPUTE_STREAM_NAME,
        computeInputVars
      ));

      connection.voidEval("abundanceDT <- rankedAbundance(" + COMPUTE_STREAM_NAME + ", " + 
                                                      singleQuote(computeEntityIdColName) + ", " +
                                                      singleQuote(method) + ")");
      connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME,
          computeEntityIdVarSpec,
          spec.getOverlayVariable(),
          util.getVariableSpecFromList(spec.getFacetVariable(), 0),
          util.getVariableSpecFromList(spec.getFacetVariable(), 1)));

      connection.voidEval("vizData <- merge(abundanceDT, " + 
          DEFAULT_SINGLE_STREAM_NAME + 
          ", by=" + singleQuote(computeEntityIdColName) +")");

      connection.voidEval(getVoidEvalVariableMetadataList(varMap));
      //there should only be a single computed collection for ranked abundance
      connection.voidEval("variables[[length(variables) + 1]] <- attributes(abundanceDT)$computedVariable[[1]]");
      String command = "plot.data::box(vizData, variables, '" +
          spec.getPoints().getValue() + "', " +
          showMean + ", " + 
          computeStats + ", '" + 
          deprecatedShowMissingness + "', " +
          "TRUE)";
      RServeClient.streamResult(connection, command, out);
    });
  }
}
