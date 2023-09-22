package org.veupathdb.service.eda.ds.plugin.pass;

import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.Resources;
import org.veupathdb.service.eda.ds.core.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.generated.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.CLINEPI_PROJECT;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.MICROBIOME_PROJECT;

public class HeatmapPlugin extends AbstractEmptyComputePlugin<HeatmapPostRequest, HeatmapSpec> {

  @Override
  public String getDisplayName() {
    return "Heatmap";
  }

  @Override
  public String getDescription() {
    return "Visualize the magnitude of a continuous numeric variable";
  }

  @Override
  public List<String> getProjects() {
    return List.of(CLINEPI_PROJECT, MICROBIOME_PROJECT);
  }

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new EmptyComputeClassGroup(HeatmapPostRequest.class, HeatmapSpec.class);
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("zAxisVariable"), List.of("yAxisVariable"), List.of("xAxisVariable"), List.of("facetVariable"))
      .pattern()
        .element("zAxisVariable")
          .types(APIVariableType.NUMBER, APIVariableType.INTEGER)
          .description("Variable must be a number.")
        .element("yAxisVariable")
          .maxValues(1000)
          .description("Variable must have 1000 or fewer unique values and be of the same or a parent entity as the Z-axis variable.")
        .element("xAxisVariable")
          .maxValues(1000)
          .description("Variable must have 1000 or fewer unique values and be of the same or a parent entity as the Y-axis variable.")
        .element("facetVariable")
          .required(false)
          .maxVars(2)
          .maxValues(10)
          .description("Variable(s) must have 10 or fewer unique values and be of the same or a parent entity as the X-axis variable.")
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(HeatmapSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("yAxisVariable", pluginSpec.getYAxisVariable())
      .var("zAxisVariable", pluginSpec.getZAxisVariable())
      .var("facetVariable", pluginSpec.getFacetVariable()));
    // z-axis not optional if value spec is series
    if (pluginSpec.getValueSpec().equals(HeatmapSpec.ValueSpecType.SERIES) && pluginSpec.getZAxisVariable() == null) {
      throw new ValidationException("zAxisVariable required for heatmap of type 'series'.");
    }
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(HeatmapSpec pluginSpec) {
    return List.of(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getYAxisVariable())
        .addVar(pluginSpec.getZAxisVariable())
        .addVars(pluginSpec.getFacetVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      PluginUtil util = getUtil();
      HeatmapSpec spec = getPluginSpec();
      Map<String, VariableSpec> varMap = new HashMap<>();
      varMap.put("xAxis", spec.getXAxisVariable());
      varMap.put("yAxis", spec.getYAxisVariable());
      varMap.put("zAxis", spec.getZAxisVariable());
      varMap.put("facet1", util.getVariableSpecFromList(spec.getFacetVariable(), 0));
      varMap.put("facet2", util.getVariableSpecFromList(spec.getFacetVariable(), 1));
      connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME,
          spec.getXAxisVariable(),
          spec.getYAxisVariable(),
          spec.getZAxisVariable(),
          util.getVariableSpecFromList(spec.getFacetVariable(), 0),
          util.getVariableSpecFromList(spec.getFacetVariable(), 1)));
      connection.voidEval(getVoidEvalVariableMetadataList(varMap));
      String cmd = "plot.data::heatmap(" + DEFAULT_SINGLE_STREAM_NAME + ", variables, '" + spec.getValueSpec().toString().toLowerCase() + "')";
      streamResult(connection, cmd, out);
    });
  }
}
