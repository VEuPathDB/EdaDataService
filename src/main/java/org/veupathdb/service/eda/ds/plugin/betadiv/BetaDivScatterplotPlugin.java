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
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.plugin.AbstractPluginWithSynchronousCompute;
import org.veupathdb.service.eda.common.plugin.util.RServeClient;
import org.veupathdb.service.eda.generated.model.BetaDivComputeConfig;
import org.veupathdb.service.eda.generated.model.BetaDivScatterplotPostRequest;
import org.veupathdb.service.eda.generated.model.BetaDivScatterplotSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;

public class BetaDivScatterplotPlugin extends AbstractPluginWithSynchronousCompute<BetaDivScatterplotPostRequest, BetaDivScatterplotSpec, BetaDivComputeConfig> {

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
      .dependencyOrder(List.of("overlayVariable"))
      .pattern()
        // .element("yAxisVariable")
        //   .types(APIVariableType.NUMBER, APIVariableType.DATE) 
        //   .description("Variable must be a number or date.")
        // .element("xAxisVariable")
        //   .types(APIVariableType.NUMBER, APIVariableType.DATE)
        //   .description("Variable must be a number or date and be of the same or a parent entity as the Y-axis variable.")
        .element("overlayVariable")
          .required(false)
          .maxValues(8)
          .description("Variable must be of the same or a parent entity as the X-axis variable.")
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(BetaDivScatterplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      // .var("xAxisVariable", pluginSpec.getXAxisVariable())
      // .var("yAxisVariable", pluginSpec.getYAxisVariable())
      .var("overlayVariable", pluginSpec.getOverlayVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(BetaDivScatterplotSpec pluginSpec, BetaDivComputeConfig computeConfig) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        // .addVar(pluginSpec.getXAxisVariable())
        // .addVar(pluginSpec.getYAxisVariable())
        .addVar(pluginSpec.getOverlayVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    BetaDivScatterplotSpec spec = getPluginSpec();
    Map<String, VariableSpec> varMap = new HashMap<>();
    // varMap.put("xAxisVariable", spec.getXAxisVariable()); // Will come from compute service
    // varMap.put("yAxisVariable", spec.getYAxisVariable());  // Will come from compute service
    varMap.put("overlayVariable", spec.getOverlayVariable());
    String valueSpec = "raw";
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "noVariables";
    String deprecatedShowMissingness = showMissingness.equals("FALSE") ? "noVariables" : showMissingness.equals("TRUE") ? "strataVariables" : showMissingness;
    
    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(getUtil().getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME,
          // spec.getXAxisVariable(), // Will come from CS
          // spec.getYAxisVariable(), // Will come from CS
          spec.getOverlayVariable()));
      connection.voidEval(getVoidEvalVarMetadataMap(DEFAULT_SINGLE_STREAM_NAME, varMap));
      String command = "plot.data::scattergl(data, map, '" + valueSpec + "', '" + deprecatedShowMissingness + "')";
      RServeClient.streamResult(connection, command, out);
    }); 
  }
}
