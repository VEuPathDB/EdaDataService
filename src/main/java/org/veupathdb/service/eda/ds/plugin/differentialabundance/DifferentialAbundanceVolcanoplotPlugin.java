package org.veupathdb.service.eda.ds.plugin.differentialabundance;

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

public class DifferentialAbundanceVolcanoplotPlugin extends AbstractPlugin<DifferentialAbundanceVolcanoplotPostRequest, DifferentialAbundanceVolcanoplotSpec, DifferentialAbundanceComputeConfig> {
  
  @Override
  public String getDisplayName() {
    return "Volcano plot";
  }

  @Override
  public String getDescription() {
    return "Display fold change vs. significance for a differential abundance analysis.";
  }

  @Override
  public List<String> getProjects() {
    return List.of(AppsMetadata.MICROBIOME_PROJECT);
  }

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new ClassGroup(DifferentialAbundanceVolcanoplotPostRequest.class, DifferentialAbundanceVolcanoplotSpec.class, DifferentialAbundanceComputeConfig.class);
  }

  // I think we can remove this whole thing ?!? there are no variables for the user to select
  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("yAxisVariable", "xAxisVariable"), List.of("overlayVariable"))
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

  // and this??
  @Override
  protected void validateVisualizationSpec(DifferentialAbundanceVolcanoplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("overlayVariable", pluginSpec.getOverlayVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(DifferentialAbundanceVolcanoplotSpec pluginSpec) {
    return List.of(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getOverlayVariable())
        .setIncludeComputedVars(true)
    );
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    DifferentialAbundanceVolcanoplotSpec spec = getPluginSpec();
    VolcanoplotPluginStats stats = getComputeResultStats(VolcanoplotPluginStats.class);

    System.out.println(stats);

    // useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
    //   connection.voidEval(getUtil().getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME,
    //       xComputedVarSpec,
    //       yComputedVarSpec,
    //       spec.getOverlayVariable()));

    //   connection.voidEval(getVoidEvalVariableMetadataList(varMap));
    //   connection.voidEval(getVoidEvalComputedVariableMetadataList(metadata));
    //   connection.voidEval("variables <- veupathUtils::merge(variables, computedVariables)");

    //   String command = "plot.data::scattergl(" + DEFAULT_SINGLE_STREAM_NAME + ", variables, '" + valueSpec + "', NULL, TRUE, TRUE, '" + deprecatedShowMissingness + "')";
    //   RServeClient.streamResult(connection, command, out);
    // }); 
  }
}
