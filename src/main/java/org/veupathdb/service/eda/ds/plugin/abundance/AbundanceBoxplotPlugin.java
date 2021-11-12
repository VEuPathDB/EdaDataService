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
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.constraints.DataElementSet;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.plugin.AbstractPluginWithCompute;
import org.veupathdb.service.eda.ds.util.RServeClient;
import org.veupathdb.service.eda.generated.model.AbundanceBoxplotPostRequest;
import org.veupathdb.service.eda.generated.model.AbundanceBoxplotSpec;
import org.veupathdb.service.eda.generated.model.AbundanceComputeConfig;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class AbundanceBoxplotPlugin extends AbstractPluginWithCompute<AbundanceBoxplotPostRequest, AbundanceBoxplotSpec, AbundanceComputeConfig> {

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
  public Integer getMaxPanels() {
    return 25;
  }
  
  @Override
  protected Class<AbundanceBoxplotSpec> getVisualizationSpecClass() {
    return AbundanceBoxplotSpec.class;
  }

  @Override
  protected Class<AbundanceComputeConfig> getComputeSpecClass() {
    return AbundanceComputeConfig.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      // TODO how to say these all have to be dependent on the collectionVar in computeConfig?
      .dependencyOrder("overlayVariable", "facetVariable")
      .pattern()
        .element("overlayVariable")
          .required(false)
          .maxValues(8)
          .description("Variable must have 8 or fewer unique values and be the same or a parent entity of the X-axis variable.")
        .element("facetVariable")
          .required(false)
          .maxVars(2)
          .description("Variable(s) must have 25 or fewer cartesian products and be of the same or a parent entity of the Overlay variable.")
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(AbundanceBoxplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("overlayVariable", pluginSpec.getOverlayVariable())
      .var("facetVariable", pluginSpec.getFacetVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(AbundanceBoxplotSpec pluginSpec, AbundanceComputeConfig computeConfig) {
    List<StreamSpec> requestedStreamsList = ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
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
    AbundanceComputeConfig computeConfig = getComputeConfig();
    AbundanceBoxplotSpec spec = getPluginSpec();
    Map<String, VariableSpec> varMap = new HashMap<>();
    varMap.put("overlayVariable", spec.getOverlayVariable());
    varMap.put("facetVariable1", getVariableSpecFromList(spec.getFacetVariable(), 0));
    varMap.put("facetVariable2", getVariableSpecFromList(spec.getFacetVariable(), 1));
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "FALSE";
    String computeStats = spec.getComputeStats() != null ? spec.getComputeStats().getValue() : "TRUE";
    String showMean = spec.getMean() != null ? spec.getMean().getValue() : "FALSE";
    String method = spec.getRankingMethod().getValue();
    String computeEntityIdColName = toColNameOrEmpty(getComputeEntityIdVarSpec(computeConfig.getCollectionVariable().getEntityId()));

    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      List<VariableSpec> computeInputVars = ListBuilder.asList(getComputeEntityIdVarSpec(computeConfig.getCollectionVariable().getEntityId()));
      computeInputVars.addAll(getChildrenVariables(computeConfig.getCollectionVariable()));
      connection.voidEval(getVoidEvalFreadCommand(COMPUTE_STREAM_NAME,
        computeInputVars
      ));
      connection.voidEval("abundanceDT <- rankedAbundance(" + COMPUTE_STREAM_NAME + ", " + 
                                                      computeEntityIdColName + ", " + 
                                                      method + ")");
      // TODO need to make sure id col to merge on is here too
      connection.voidEval(getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME, 
          spec.getOverlayVariable(),
          getVariableSpecFromList(spec.getFacetVariable(), 0),
          getVariableSpecFromList(spec.getFacetVariable(), 1)));
      connection.voidEval("vizData <- merge(abundanceDT, " + 
          DEFAULT_SINGLE_STREAM_NAME + 
       ", by=" + computeEntityIdColName +")");
      connection.voidEval(getVoidEvalVarMetadataMap(DEFAULT_SINGLE_STREAM_NAME, varMap));
      connection.voidEval("map <- rbind(map, list('id'=attributes(abundanceDT)$computedVariableDetails$variableId," +
                                                 "'plotRef'=rep('xAxisVariable', length(attributes(abundanceDT)$computedVariableDetails$variableId))," +
                                                 "'dataType'=attributes(abundanceDT)$computedVariableDetails$dataType," +
                                                 "'dataShape'=attributes(abundanceDT)$computedVariableDetails$dataShape");
      String command = "plot.data::box(vizData, map, '" +
          spec.getPoints().getValue() + "', " +
          showMean + ", " + 
          computeStats + ", " + 
          showMissingness + ", " +
          "'xAxisVariable', " + // x only initially, confusing ux and api otherwise?
          "NULL, " + // getting display label for collection var might be client side work?
          "'Abundance')";
      RServeClient.streamResult(connection, command, out);
    });
  }
}
