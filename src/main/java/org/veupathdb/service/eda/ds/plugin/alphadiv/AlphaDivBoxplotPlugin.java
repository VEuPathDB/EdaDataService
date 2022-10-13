package org.veupathdb.service.eda.ds.plugin.alphadiv;

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
import org.veupathdb.service.eda.generated.model.AlphaDivBoxplotPostRequest;
import org.veupathdb.service.eda.generated.model.BoxplotWith1ComputeSpec;
import org.veupathdb.service.eda.generated.model.AlphaDivComputeConfig;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.common.plugin.util.PluginUtil.singleQuote;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;

public class AlphaDivBoxplotPlugin extends AbstractPluginWithCompute<AlphaDivBoxplotPostRequest, BoxplotWith1ComputeSpec, AlphaDivComputeConfig> {

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
  protected Class<BoxplotWith1ComputeSpec> getVisualizationSpecClass() {
    return BoxplotWith1ComputeSpec.class;
  }

  @Override
  protected Class<AlphaDivComputeConfig> getComputeSpecClass() {
    return AlphaDivComputeConfig.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(new String[] {"xAxisVariable"}, new String[] {"overlayVariable", "facetVariable"})
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
  protected List<StreamSpec> getRequestedStreams(BoxplotWith1ComputeSpec pluginSpec, AlphaDivComputeConfig computeConfig) {
    List<StreamSpec> requestedStreamsList = ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
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
    AlphaDivComputeConfig computeConfig = getComputeConfig();
    BoxplotWith1ComputeSpec spec = getPluginSpec();
    PluginUtil util = getUtil();
    Map<String, VariableSpec> varMap = new HashMap<>();
    varMap.put("xAxisVariable", spec.getXAxisVariable());
    varMap.put("overlayVariable", spec.getOverlayVariable());
    varMap.put("facetVariable1", util.getVariableSpecFromList(spec.getFacetVariable(), 0));
    varMap.put("facetVariable2", util.getVariableSpecFromList(spec.getFacetVariable(), 1));
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "noVariables";
    String deprecatedShowMissingness = showMissingness.equals("FALSE") ? "noVariables" : showMissingness.equals("TRUE") ? "strataVariables" : showMissingness;
    String computeStats = spec.getComputeStats() != null ? spec.getComputeStats().getValue() : "TRUE";
    String showMean = spec.getMean() != null ? spec.getMean().getValue() : "FALSE";
    String method = computeConfig.getAlphaDivMethod().getValue();
    VariableDef computeEntityIdVarSpec = util.getEntityIdVarSpec(computeConfig.getCollectionVariable().getEntityId());
    String computeEntityIdColName = util.toColNameOrEmpty(computeEntityIdVarSpec);
    
    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      List<VariableSpec> computeInputVars = ListBuilder.asList(computeEntityIdVarSpec);
      computeInputVars.addAll(util.getChildrenVariables(computeConfig.getCollectionVariable()));
      connection.voidEval(util.getVoidEvalFreadCommand(COMPUTE_STREAM_NAME,
        computeInputVars
      ));
      connection.voidEval("print("+ COMPUTE_STREAM_NAME + ")");
      connection.voidEval("alphaDivDT <- alphaDiv(" + COMPUTE_STREAM_NAME + ", " + 
                                                      singleQuote(computeEntityIdColName) + ", " + 
                                                      singleQuote(method) + ")");
      connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME,
          computeEntityIdVarSpec,
          spec.getXAxisVariable(),
          spec.getOverlayVariable(),
          util.getVariableSpecFromList(spec.getFacetVariable(), 0),
          util.getVariableSpecFromList(spec.getFacetVariable(), 1)));
      
      // merge alpha div and other viz stream data as input to boxplot
      connection.voidEval("vizData <- merge(alphaDivDT, " + 
                                            DEFAULT_SINGLE_STREAM_NAME + 
                                         ", by=" + singleQuote(computeEntityIdColName) +")");
      connection.voidEval(getVoidEvalVarMetadataMap(DEFAULT_SINGLE_STREAM_NAME, varMap));
      // update the new map obj in R to add alphaDiv
      connection.voidEval("map <- rbind(map, list('id'=veupathUtils::toColNameOrNull(attributes(alphaDivDT)$computedVariable$computedVariableDetails)," +
                                                 "'plotRef'='yAxisVariable'," +
                                                 "'dataType'=attributes(alphaDivDT)$computedVariable$computedVariableDetails$dataType," +
                                                 "'dataShape'=attributes(alphaDivDT)$computedVariable$computedVariableDetails$dataShape))");
      String command = "plot.data::box(vizData, map, '" +
          spec.getPoints().getValue() + "', " +
          showMean + ", " + 
          computeStats + ", '" + 
          deprecatedShowMissingness + "', " +
          "computedVariableMetadata=attributes(alphaDivDT)$computedVariable$computedVariableMetadata)";
      RServeClient.streamResult(connection, command, out);
    });
  }
}
