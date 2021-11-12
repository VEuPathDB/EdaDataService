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
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.constraints.DataElementSet;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.plugin.AbstractPluginWithCompute;
import org.veupathdb.service.eda.ds.util.RServeClient;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.AlphaDivComputeConfig;
import org.veupathdb.service.eda.generated.model.AlphaDivScatterplotPostRequest;
import org.veupathdb.service.eda.generated.model.AlphaDivScatterplotSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class AlphaDivScatterplotPlugin extends AbstractPluginWithCompute<AlphaDivScatterplotPostRequest, AlphaDivScatterplotSpec, AlphaDivComputeConfig> {

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
  public Integer getMaxPanels() {
    return 25;
  }
  
  @Override
  protected Class<AlphaDivScatterplotSpec> getVisualizationSpecClass() {
    return AlphaDivScatterplotSpec.class;
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
          .types(APIVariableType.NUMBER, APIVariableType.DATE)
          .description("Variable must be a number or date and be of the same or a parent entity as the Y-axis variable.")
        .element("overlayVariable")
          .description("Variable must be of the same or a parent entity as the X-axis variable.")
        .element("facetVariable")
          .required(false)
          .maxVars(2)
          .description("Variable(s) must have 25 or fewer cartesian products and be of the same or a parent entity as the Overlay variable.")
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(AlphaDivScatterplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("overlayVariable", pluginSpec.getOverlayVariable())
      .var("facetVariable", pluginSpec.getFacetVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(AlphaDivScatterplotSpec pluginSpec, AlphaDivComputeConfig computeConfig) {
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
    AlphaDivScatterplotSpec spec = getPluginSpec();
    Map<String, VariableSpec> varMap = new HashMap<>();
    varMap.put("xAxisVariable", spec.getXAxisVariable());
    varMap.put("overlayVariable", spec.getOverlayVariable());
    varMap.put("facetVariable1", getVariableSpecFromList(spec.getFacetVariable(), 0));
    varMap.put("facetVariable2", getVariableSpecFromList(spec.getFacetVariable(), 1));
    String valueSpec = spec.getValueSpec().getValue();
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "FALSE";
    String method = spec.getAlphaDivMethod().getValue();
    String computeEntityIdColName = toColNameOrEmpty(getComputeEntityIdVarSpec(computeConfig.getCollectionVariable().getEntityId()));
    
    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      List<VariableSpec> computeInputVars = ListBuilder.asList(getComputeEntityIdVarSpec(computeConfig.getCollectionVariable().getEntityId()));
      computeInputVars.addAll(getChildrenVariables(computeConfig.getCollectionVariable()));
      connection.voidEval(getVoidEvalFreadCommand(COMPUTE_STREAM_NAME,
        computeInputVars
      ));
      connection.voidEval("alphaDivDT <- alphaDiv(" + COMPUTE_STREAM_NAME + ", " + 
                                                      computeEntityIdColName + ", " + 
                                                      method + ")");
      connection.voidEval(getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME, 
          spec.getXAxisVariable(),
          spec.getOverlayVariable(),
          getVariableSpecFromList(spec.getFacetVariable(), 0),
          getVariableSpecFromList(spec.getFacetVariable(), 1)));
      connection.voidEval("vizData <- merge(alphaDivDT, " + 
          DEFAULT_SINGLE_STREAM_NAME + 
       ", by=" + computeEntityIdColName +")");
      connection.voidEval(getVoidEvalVarMetadataMap(DEFAULT_SINGLE_STREAM_NAME, varMap));
      connection.voidEval("map <- rbind(map, list('id'=veupathUtils::toColNameOrNull(attributes(alphaDivDT)$computedVariableDetails)," +
                                                 "'plotRef'='yAxisVariable'," +
                                                 "'dataType'=attributes(alphaDivDT)$computedVariableDetails$dataType," +
                                                 "'dataShape'=attributes(alphaDivDT)$computedVariableDetails$dataShape," +
                                                 "'displayLabel'=attributes(alphaDivDT)$computedVariableDetails$displayLabel))");
      String command = "plot.data::scattergl(vizData, map, '" + valueSpec + "', " + showMissingness + ")";
      RServeClient.streamResult(connection, command, out);
    }); 
  }
}
