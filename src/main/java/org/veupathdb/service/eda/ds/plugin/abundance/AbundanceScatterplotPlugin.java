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
  public Integer getMaxPanels() {
    return 25;
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
      .dependencyOrder("xAxisVariable", "overlayVariable", "facetVariable")
      .pattern()
        // .element("yAxisVariable")
        //   .types(APIVariableType.NUMBER, APIVariableType.DATE) 
        //   .description("Variable must be a number or date.")
        .element("xAxisVariable")
          .types(APIVariableType.NUMBER, APIVariableType.DATE)
          .description("Variable must be a number or date and be of the same or a parent entity as the Y-axis variable.")
        .element("overlayVariable")
          .maxVars(8)
          .description("Variable must have 8 or fewer values and be of the same or a parent entity as the X-axis variable.")
        .element("facetVariable")
          .required(false)
          .maxVars(2)
          .description("Variable(s) must have 25 or fewer cartesian products and be of the same or a parent entity as the Overlay variable.")
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(AbundanceScatterplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      // .var("yAxisVariable", pluginSpec.getYAxisVariable())
      .var("overlayVariable", pluginSpec.getOverlayVariable())
      .var("facetVariable", pluginSpec.getFacetVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(AbundanceScatterplotSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        // .addVar(pluginSpec.getYAxisVariable())
        .addVars(pluginSpec.getOverlayVariable())
        .addVars(pluginSpec.getFacetVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    AbundanceScatterplotSpec spec = getPluginSpec();
    Map<String, VariableSpec> varMap = new HashMap<>();
    varMap.put("xAxisVariable", spec.getXAxisVariable());
    // varMap.put("yAxisVariable", spec.getYAxisVariable());
    // varMap.put("overlayVariable", spec.getOverlayVariable());
    varMap.put("facetVariable1", getVariableSpecFromList(spec.getFacetVariable(), 0));
    varMap.put("facetVariable2", getVariableSpecFromList(spec.getFacetVariable(), 1));
    String valueSpec = spec.getValueSpec().getValue();
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "FALSE";
    String listVarPlotRef = "overlayVariable"; // soon will be part of the viz post request
    String listVarDisplayLabel = "Phylum"; // get x axis label from compute output.
    String inferredVarDisplayLabel = "Abundance"; // Also could grab from the compute output.
    // String yVarType = getVariableType(spec.getYAxisVariable());
    
    // if (yVarType.equals("DATE") && !valueSpec.equals("raw")) {
    //   LOG.error("Cannot calculate trend lines for y-axis date variables. The `valueSpec` property must be set to `raw`.");
    // }
    
    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      connection.voidEval(getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME, 
          spec.getXAxisVariable(),
          // spec.getYAxisVariable(),
          // spec.getOverlayVariable(),
          getVariableSpecFromList(spec.getFacetVariable(), 0),
          getVariableSpecFromList(spec.getFacetVariable(), 1)));
      connection.voidEval(getVoidEvalVarMetadataMap(DEFAULT_SINGLE_STREAM_NAME, varMap));
      String command = "plot.data::scattergl(data, map, '" +
          valueSpec + "', " + 
          showMissingness + ",'" +
          listVarPlotRef + "','" +
          listVarDisplayLabel + "','" +
          inferredVarDisplayLabel + "')";
      RServeClient.streamResult(connection, command, out);
    }); 
  }
}