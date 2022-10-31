package org.veupathdb.service.eda.ds.plugin.alphadiv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.common.plugin.util.RServeClient;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.AlphaDivComputeConfig;
import org.veupathdb.service.eda.generated.model.AlphaDivScatterplotPostRequest;
import org.veupathdb.service.eda.generated.model.ScatterplotWith1ComputeSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.common.plugin.util.PluginUtil.singleQuote;
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
  protected Class<ScatterplotWith1ComputeSpec> getVisualizationSpecClass() {
    return ScatterplotWith1ComputeSpec.class;
  }

  @Override
  protected Class<AlphaDivComputeConfig> getComputeConfigClass() {
    return AlphaDivComputeConfig.class;
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
    AlphaDivComputeConfig computeConfig = getComputeConfig();
    List<StreamSpec> requestedStreamsList = List.of(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getOverlayVariable())
        .addVars(pluginSpec.getFacetVariable()),
      new StreamSpec(COMPUTE_STREAM_NAME, computeConfig.getCollectionVariable().getEntityId())
        .addVars(getUtil().getChildrenVariables(computeConfig.getCollectionVariable()))
    );
    return requestedStreamsList;
  }

  @Override
  protected boolean includeComputedVarsInStream() {
    return false;
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    AlphaDivComputeConfig computeConfig = getComputeConfig();
    ScatterplotWith1ComputeSpec spec = getPluginSpec();
    PluginUtil util = getUtil();
    Map<String, VariableSpec> varMap = new HashMap<>();
    varMap.put("xAxisVariable", spec.getXAxisVariable());
    varMap.put("overlayVariable", spec.getOverlayVariable());
    varMap.put("facetVariable1", util.getVariableSpecFromList(spec.getFacetVariable(), 0));
    varMap.put("facetVariable2", util.getVariableSpecFromList(spec.getFacetVariable(), 1));
    String valueSpec = spec.getValueSpec().getValue();
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "noVariables";
    String deprecatedShowMissingness = showMissingness.equals("FALSE") ? "noVariables" : showMissingness.equals("TRUE") ? "strataVariables" : showMissingness;
    String method = computeConfig.getAlphaDivMethod().getValue();
    VariableDef computeEntityIdVarSpec = util.getEntityIdVarSpec(computeConfig.getCollectionVariable().getEntityId());
    String computeEntityIdColName = util.toColNameOrEmpty(computeEntityIdVarSpec);
    
    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      List<VariableSpec> computeInputVars = ListBuilder.asList(computeEntityIdVarSpec);
      computeInputVars.addAll(util.getChildrenVariables(computeConfig.getCollectionVariable()));
      connection.voidEval(util.getVoidEvalFreadCommand(COMPUTE_STREAM_NAME,
        computeInputVars
      ));
      connection.voidEval("alphaDivDT <- alphaDiv(" + COMPUTE_STREAM_NAME + ", " + 
                                                      singleQuote(computeEntityIdColName) + ", " + 
                                                      singleQuote(method) + ")");
      connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME,
          computeEntityIdVarSpec,
          spec.getXAxisVariable(),
          spec.getOverlayVariable(),
          util.getVariableSpecFromList(spec.getFacetVariable(), 0),
          util.getVariableSpecFromList(spec.getFacetVariable(), 1)));
      connection.voidEval("vizData <- merge(alphaDivDT, " + 
          DEFAULT_SINGLE_STREAM_NAME + 
       ", by=" + singleQuote(computeEntityIdColName) +")");
      connection.voidEval(getVoidEvalVarMetadataMap(DEFAULT_SINGLE_STREAM_NAME, varMap));
      connection.voidEval("map <- rbind(map, list('id'=veupathUtils::toColNameOrNull(attributes(alphaDivDT)$computedVariable$computedVariableDetails)," +
                                                 "'plotRef'='yAxisVariable'," +
                                                 "'dataType'=attributes(alphaDivDT)$computedVariable$computedVariableDetails$dataType," +
                                                 "'dataShape'=attributes(alphaDivDT)$computedVariable$computedVariableDetails$dataShape))");
      String command = "plot.data::scattergl(vizData, map, '" + valueSpec + "', '" + deprecatedShowMissingness + "', computedVariableMetadata=attributes(alphaDivDT)$computedVariable$computedVariableMetadata)";
      RServeClient.streamResult(connection, command, out);
    }); 
  }
}
