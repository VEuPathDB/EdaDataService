package org.veupathdb.service.eda.ds.plugin.pass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.constraints.DataElementSet;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.HeatmapPostRequest;
import org.veupathdb.service.eda.generated.model.HeatmapSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.ds.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class HeatmapPlugin extends AbstractPlugin<HeatmapPostRequest, HeatmapSpec> {

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
    return Arrays.asList("ClinEpiDB", "MicrobiomeDB");
  }
  
  @Override
  public Integer getMaxPanels() {
    return 25;
  }
  
  @Override
  protected Class<HeatmapSpec> getVisualizationSpecClass() {
    return HeatmapSpec.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder("zAxisVariable", "yAxisVariable", "xAxisVariable", "facetVariable")
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
          .description("Variable(s) must have 25 or fewer cartesian products and be of the same or a parent entity as the X-axis variable.")
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
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getYAxisVariable())
        .addVar(pluginSpec.getZAxisVariable())
        .addVars(pluginSpec.getFacetVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      HeatmapSpec spec = getPluginSpec();
      Map<String, VariableSpec> varMap = new HashMap<String, VariableSpec>();
      varMap.put("xAxisVariable", spec.getXAxisVariable());
      varMap.put("yAxisVariable", spec.getYAxisVariable());
      varMap.put("zAxisVariable", spec.getZAxisVariable());
      varMap.put("facetVariable1", getVariableSpecFromList(spec.getFacetVariable(), 0));
      varMap.put("facetVariable2", getVariableSpecFromList(spec.getFacetVariable(), 1));
      connection.voidEval(getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME, 
          spec.getXAxisVariable(),
          spec.getYAxisVariable(),
          spec.getZAxisVariable(),
          getVariableSpecFromList(spec.getFacetVariable(), 0),
          getVariableSpecFromList(spec.getFacetVariable(), 1)));
      connection.voidEval(getVoidEvalVarMetadataMap(DEFAULT_SINGLE_STREAM_NAME, varMap));
      String cmd = "plot.data::heatmap(" + DEFAULT_SINGLE_STREAM_NAME + ", map, '" + spec.getValueSpec().toString().toLowerCase() + "')";
      streamResult(connection, cmd, out);
    });
  }
}