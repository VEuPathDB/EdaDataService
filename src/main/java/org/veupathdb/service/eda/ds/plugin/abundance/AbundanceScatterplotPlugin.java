package org.veupathdb.service.eda.ds.plugin.abundance;

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
import org.veupathdb.service.eda.generated.model.AbundanceComputeConfig;
import org.veupathdb.service.eda.generated.model.AbundanceScatterplotPostRequest;
import org.veupathdb.service.eda.generated.model.AbundanceScatterplotSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class AbundanceScatterplotPlugin extends AbstractPluginWithCompute<AbundanceScatterplotPostRequest, AbundanceScatterplotSpec, AbundanceComputeConfig> {

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
  protected Class<AbundanceScatterplotSpec> getVisualizationSpecClass() {
    return AbundanceScatterplotSpec.class;
  }

  @Override
  protected Class<AbundanceComputeConfig> getComputeSpecClass() {
    return AbundanceComputeConfig.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder("xAxisVariable", "facetVariable")
      .pattern()
        .element("xAxisVariable")
          .types(APIVariableType.NUMBER, APIVariableType.DATE)
          .description("Variable must be a number or date and be of the same or a parent entity as the Y-axis variable.")
        .element("facetVariable")
          .required(false)
          .maxVars(2)
          .maxValues(7)
          .description("Variable(s) must have7 or fewer unique values and be of the same or a parent entity as the Overlay variable.")
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(AbundanceScatterplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("facetVariable", pluginSpec.getFacetVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(AbundanceScatterplotSpec pluginSpec, AbundanceComputeConfig computeConfig) {
    List<StreamSpec> requestedStreamsList = ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
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
    AbundanceComputeConfig computeConfig = getComputeConfig();
    AbundanceScatterplotSpec spec = getPluginSpec();
    Map<String, VariableSpec> varMap = new HashMap<>();
    varMap.put("xAxisVariable", spec.getXAxisVariable());
    varMap.put("facetVariable1", getVariableSpecFromList(spec.getFacetVariable(), 0));
    varMap.put("facetVariable2", getVariableSpecFromList(spec.getFacetVariable(), 1));
    String valueSpec = spec.getValueSpec().getValue();
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "FALSE";
    String method = spec.getRankingMethod().getValue();
    VariableSpec computeEntityIdVarSpec = getComputeEntityIdVarSpec(computeConfig.getCollectionVariable().getEntityId());
    String computeEntityIdColName = toColNameOrEmpty(computeEntityIdVarSpec); 

    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      List<VariableSpec> computeInputVars = ListBuilder.asList(computeEntityIdVarSpec);
      computeInputVars.addAll(getChildrenVariables(computeConfig.getCollectionVariable()));
      connection.voidEval(getVoidEvalFreadCommand(COMPUTE_STREAM_NAME,
        computeInputVars
      ));
      connection.voidEval("abundanceDT <- rankedAbundance(" + COMPUTE_STREAM_NAME + ", " + 
                                                      computeEntityIdColName + ", " + 
                                                      method + ", 8)");
      connection.voidEval(getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME, 
          computeEntityIdVarSpec,
          spec.getXAxisVariable(),
          getVariableSpecFromList(spec.getFacetVariable(), 0),
          getVariableSpecFromList(spec.getFacetVariable(), 1)));
      connection.voidEval("vizData <- merge(abundanceDT, " + 
          DEFAULT_SINGLE_STREAM_NAME + 
       ", by=" + computeEntityIdColName +")");
      connection.voidEval(getVoidEvalVarMetadataMap(DEFAULT_SINGLE_STREAM_NAME, varMap));
      connection.voidEval("map <- rbind(map, list('id'=veupathUtils::toColNameOrNull(attributes(abundanceDT)$computedVariableDetails)," +
                                                 "'plotRef'=rep('xAxisVariable', length(attributes(abundanceDT)$computedVariableDetails$variableId))," +
                                                 "'dataType'=attributes(abundanceDT)$computedVariableDetails$dataType," +
                                                 "'dataShape'=attributes(abundanceDT)$computedVariableDetails$dataShape");
      String command = "plot.data::scattergl(vizData, map, '" +
          valueSpec + "', " + 
          showMissingness + ", " +
          "'overlayVariable', " +
          "NULL, " +
          "'Abundance')";
      RServeClient.streamResult(connection, command, out);
    }); 
  }
}
