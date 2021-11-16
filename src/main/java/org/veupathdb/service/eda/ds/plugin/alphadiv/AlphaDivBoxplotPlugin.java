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
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.constraints.DataElementSet;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.plugin.AbstractPluginWithCompute;
import org.veupathdb.service.eda.ds.util.RServeClient;
import org.veupathdb.service.eda.generated.model.AlphaDivBoxplotPostRequest;
import org.veupathdb.service.eda.generated.model.AlphaDivBoxplotSpec;
import org.veupathdb.service.eda.generated.model.AlphaDivComputeConfig;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class AlphaDivBoxplotPlugin extends AbstractPluginWithCompute<AlphaDivBoxplotPostRequest, AlphaDivBoxplotSpec, AlphaDivComputeConfig> {

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
  protected Class<AlphaDivBoxplotSpec> getVisualizationSpecClass() {
    return AlphaDivBoxplotSpec.class;
  }

  @Override
  protected Class<AlphaDivComputeConfig> getComputeSpecClass() {
    return AlphaDivComputeConfig.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder("xAxisVariable", "overlayVariable", "facetVariable")
      .pattern()
        .element("xAxisVariable")
          .maxValues(10)
          .description("Variable must have 10 or fewer unique values and be the same or a parent entity of the Y-axis variable.") // Of taxa entity?
        .element("overlayVariable")
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
  protected void validateVisualizationSpec(AlphaDivBoxplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("overlayVariable", pluginSpec.getOverlayVariable())
      .var("facetVariable", pluginSpec.getFacetVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(AlphaDivBoxplotSpec pluginSpec, AlphaDivComputeConfig computeConfig) {
    List<StreamSpec> requestedStreamsList = ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getOverlayVariable())
        .addVars(pluginSpec.getFacetVariable())
      );
    requestedStreamsList.add(
      new StreamSpec(COMPUTE_STREAM_NAME, computeConfig.getCollectionVariable().getEntityId())
        .addVars(getChildrenVariables(computeConfig.getCollectionVariable()) 
      ));
    
    return requestedStreamsList;
  }

  @Override
 protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    AlphaDivComputeConfig computeConfig = getComputeConfig();
    AlphaDivBoxplotSpec spec = getPluginSpec();
    Map<String, VariableSpec> varMap = new HashMap<>();
    varMap.put("xAxisVariable", spec.getXAxisVariable());
    varMap.put("overlayVariable", spec.getOverlayVariable());
    varMap.put("facetVariable1", getVariableSpecFromList(spec.getFacetVariable(), 0));
    varMap.put("facetVariable2", getVariableSpecFromList(spec.getFacetVariable(), 1));
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "FALSE";
    String computeStats = spec.getComputeStats() != null ? spec.getComputeStats().getValue() : "TRUE";
    String showMean = spec.getMean() != null ? spec.getMean().getValue() : "FALSE";
    String method = spec.getAlphaDivMethod().getValue();
    VariableSpec computeEntityIdVarSpec = getComputeEntityIdVarSpec(computeConfig.getCollectionVariable().getEntityId());
    String computeEntityIdColName = toColNameOrEmpty(computeEntityIdVarSpec);
    
    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      List<VariableSpec> computeInputVars = ListBuilder.asList(computeEntityIdVarSpec);
      computeInputVars.addAll(getChildrenVariables(computeConfig.getCollectionVariable()));
      connection.voidEval(getVoidEvalFreadCommand(COMPUTE_STREAM_NAME,
        computeInputVars
      ));
      connection.voidEval("alphaDivDT <- alphaDiv(" + COMPUTE_STREAM_NAME + ", " + 
                                                      computeEntityIdColName + ", " + 
                                                      method + ")");
      connection.voidEval(getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME,
          computeEntityIdVarSpec,
          spec.getXAxisVariable(),
          spec.getOverlayVariable(),
          getVariableSpecFromList(spec.getFacetVariable(), 0),
          getVariableSpecFromList(spec.getFacetVariable(), 1)));
      // merge alpha div and other viz stream data as input to boxplot
      connection.voidEval("vizData <- merge(alphaDivDT, " + 
                                            DEFAULT_SINGLE_STREAM_NAME + 
                                         ", by=" + computeEntityIdColName +")");
      connection.voidEval(getVoidEvalVarMetadataMap(DEFAULT_SINGLE_STREAM_NAME, varMap));
      // update the new map obj in R to add alphaDiv
      connection.voidEval("map <- rbind(map, list('id'=veupathUtils::toColNameOrNull(attributes(alphaDivDT)$computedVariableDetails)," +
                                                 "'plotRef'='yAxisVariable'," +
                                                 "'dataType'=attributes(alphaDivDT)$computedVariableDetails$dataType," +
                                                 "'dataShape'=attributes(alphaDivDT)$computedVariableDetails$dataShape," +
                                                 "'displayLabel'=attributes(alphaDivDT)$computedVariableDetails$displayLabel))");
      String command = "plot.data::box(vizData, map, '" +
          spec.getPoints().getValue() + "', " +
          showMean + ", " + 
          computeStats + ", " + 
          showMissingness + ")";
      RServeClient.streamResult(connection, command, out);
    });
  }
}
