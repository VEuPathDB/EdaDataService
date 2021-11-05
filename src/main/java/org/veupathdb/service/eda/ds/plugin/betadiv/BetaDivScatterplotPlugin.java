package org.veupathdb.service.eda.ds.plugin.betadiv;

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
import org.veupathdb.service.eda.generated.model.BetaDivComputeConfig;
import org.veupathdb.service.eda.generated.model.BetaDivScatterplotPostRequest;
import org.veupathdb.service.eda.generated.model.BetaDivScatterplotSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class BetaDivScatterplotPlugin extends AbstractPluginWithCompute<BetaDivScatterplotPostRequest, BetaDivScatterplotSpec, BetaDivComputeConfig> {

  private static final Logger LOG = LogManager.getLogger(BetaDivScatterplotPlugin.class);
  
  @Override
  public String getDisplayName() {
    return "Scatter plot";
  }

  @Override
  public String getDescription() {
    return "Visualize a 2-dimensional projection of samples based on their beta diversitiy";
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
  protected Class<BetaDivScatterplotSpec> getVisualizationSpecClass() {
    return BetaDivScatterplotSpec.class;
  }

  @Override
  protected Class<BetaDivComputeConfig> getComputeSpecClass() {
    return BetaDivComputeConfig.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder("overlayVariable")
      .pattern()
        // .element("yAxisVariable")
        //   .types(APIVariableType.NUMBER, APIVariableType.DATE) 
        //   .description("Variable must be a number or date.")
        // .element("xAxisVariable")
        //   .types(APIVariableType.NUMBER, APIVariableType.DATE)
        //   .description("Variable must be a number or date and be of the same or a parent entity as the Y-axis variable.")
        .element("overlayVariable")
          .description("Variable must be of the same or a parent entity as the X-axis variable.")
        // .element("facetVariable")
        //   .required(false)
        //   .maxVars(2)
        //   .description("Variable(s) must have 25 or fewer cartesian products and be of the same or a parent entity as the Overlay variable.")
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(BetaDivScatterplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      // .var("xAxisVariable", pluginSpec.getXAxisVariable())
      // .var("yAxisVariable", pluginSpec.getYAxisVariable())
      .var("overlayVariable", pluginSpec.getOverlayVariable()));
      // .var("facetVariable", pluginSpec.getFacetVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(BetaDivScatterplotSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        // .addVar(pluginSpec.getXAxisVariable())
        // .addVar(pluginSpec.getYAxisVariable())
        .addVar(pluginSpec.getOverlayVariable()));
        // .addVars(pluginSpec.getFacetVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    BetaDivScatterplotSpec spec = getPluginSpec();
    Map<String, VariableSpec> varMap = new HashMap<>();
    // varMap.put("xAxisVariable", spec.getXAxisVariable()); // Will come from compute service
    // varMap.put("yAxisVariable", spec.getYAxisVariable());  // Will come from compute service
    varMap.put("overlayVariable", spec.getOverlayVariable());
    // varMap.put("facetVariable1", getVariableSpecFromList(spec.getFacetVariable(), 0));
    // varMap.put("facetVariable2", getVariableSpecFromList(spec.getFacetVariable(), 1));
    String valueSpec = "raw";
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "FALSE";
    // String yVarType = getVariableType(spec.getYAxisVariable());
    
    // if (yVarType.equals("DATE") && !valueSpec.equals("raw")) {
    //   LOG.error("Cannot calculate trend lines for y-axis date variables. The `valueSpec` property must be set to `raw`.");
    // }
    
    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      connection.voidEval(getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME, 
          // spec.getXAxisVariable(), // Will come from CS
          // spec.getYAxisVariable(), // Will come from CS
          spec.getOverlayVariable()));
          // getVariableSpecFromList(spec.getFacetVariable(), 0),
          // getVariableSpecFromList(spec.getFacetVariable(), 1)));
      connection.voidEval(getVoidEvalVarMetadataMap(DEFAULT_SINGLE_STREAM_NAME, varMap));
      String command = "plot.data::scattergl(data, map, '" + valueSpec + "', " + showMissingness + ")";
      RServeClient.streamResult(connection, command, out);
    }); 
  }
}