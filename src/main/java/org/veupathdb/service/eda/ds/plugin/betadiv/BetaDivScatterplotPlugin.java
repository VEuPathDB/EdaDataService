package org.veupathdb.service.eda.ds.plugin.betadiv;

import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.RServeClient;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;

public class BetaDivScatterplotPlugin extends AbstractPlugin<BetaDivScatterplotPostRequest, BetaDivScatterplotSpec, BetaDivComputeConfig> {
  
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
  protected ClassGroup getTypeParameterClasses() {
    return new ClassGroup(BetaDivScatterplotPostRequest.class, BetaDivScatterplotSpec.class, BetaDivComputeConfig.class);
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("overlayVariable"))
      .pattern()
        .element("overlayVariable")
          .required(false)
          .maxValues(8)
          .description("Variable must be a number, or have 8 or fewer values, and be of the same or a parent entity as the X-axis variable.")
      .pattern()
        .element("overlayVariable")
          .required(false)
          .types(APIVariableType.NUMBER, APIVariableType.INTEGER) 
          .description("Variable must be a number, or have 8 or fewer values, and be of the same or a parent entity as the X-axis variable.")
      .done();
  }

  @Override
  protected void validateVisualizationSpec(BetaDivScatterplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("overlayVariable", pluginSpec.getOverlayVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(BetaDivScatterplotSpec pluginSpec) {
    return List.of(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getOverlayVariable())
        .setIncludeComputedVars(true)
    );
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    BetaDivScatterplotSpec spec = getPluginSpec();
    Map<String, VariableSpec> varMap = new HashMap<>();
    varMap.put("overlay", spec.getOverlayVariable());
    String valueSpec = "raw";
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "noVariables";
    String deprecatedShowMissingness = showMissingness.equals("FALSE") ? "noVariables" : showMissingness.equals("TRUE") ? "strataVariables" : showMissingness;
   
    ComputedVariableMetadata metadata = getComputedVariableMetadata();
    VariableSpec xComputedVarSpec = metadata.getVariables().stream()
        .filter(var -> var.getPlotReference().getValue().equals("xAxis"))
        .findFirst().orElseThrow().getVariableSpec();
    VariableSpec yComputedVarSpec = metadata.getVariables().stream()
        .filter(var -> var.getPlotReference().getValue().equals("yAxis"))
        .findFirst().orElseThrow().getVariableSpec();

    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(getUtil().getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME,
          xComputedVarSpec,
          yComputedVarSpec,
          spec.getOverlayVariable()));

      connection.voidEval(getVoidEvalVariableMetadataList(varMap));
      connection.voidEval(getVoidEvalComputedVariableMetadataList(metadata));
      connection.voidEval("variables <- veupathUtils::merge(variables, computedVariables)");

      String command = "plot.data::scattergl(" + DEFAULT_SINGLE_STREAM_NAME + ", variables, '" + valueSpec + "', '" + deprecatedShowMissingness + "')";
      RServeClient.streamResult(connection, command, out);
    }); 
  }
}
