package org.veupathdb.service.eda.ds.plugin.pass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.MosaicPostRequest;
import org.veupathdb.service.eda.generated.model.MosaicSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.CLINEPI_PROJECT;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;

public class TwoByTwoPlugin extends AbstractPlugin<MosaicPostRequest, MosaicSpec> {

  @Override
  public String getDisplayName() {
    return "Mosaic plot, 2x2 table";
  }

  @Override
  public String getDescription() {
    return "Visualize the frequency distribution, relative risk and odds ratio for two dichotomous variables";
  }
  
  @Override
  public List<String> getProjects() {
    return List.of(CLINEPI_PROJECT);
  }

  @Override
  protected Class<MosaicSpec> getVisualizationSpecClass() {
    return MosaicSpec.class;
  }
  
  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("yAxisVariable", "xAxisVariable"), List.of("facetVariable"))
      .pattern()
        .element("yAxisVariable")
          .shapes(APIVariableDataShape.BINARY)
          .description("Variable must have exactly 2 unique values.")
        .element("xAxisVariable")
          .shapes(APIVariableDataShape.BINARY)
          .description("Variable must have exactly 2 unique values and be of the same or a parent entity as the Y-axis variable.")
        .element("facetVariable")
          .required(false)
          .maxVars(2)
          .maxValues(10)
          .description("Variable(s) must have 10 or fewer unique values and be of the same or a parent entity as the X-axis variable.")
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(MosaicSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("yAxisVariable", pluginSpec.getYAxisVariable())
      .var("facetVariable", pluginSpec.getFacetVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(MosaicSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getYAxisVariable())
        .addVars(pluginSpec.getFacetVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    PluginUtil util = getUtil();
    MosaicSpec spec = getPluginSpec();
    Map<String, VariableSpec> varMap = new HashMap<>();
    varMap.put("xAxisVariable", spec.getXAxisVariable());
    varMap.put("yAxisVariable", spec.getYAxisVariable());
    varMap.put("facetVariable1", util.getVariableSpecFromList(spec.getFacetVariable(), 0));
    varMap.put("facetVariable2", util.getVariableSpecFromList(spec.getFacetVariable(), 1));
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "noVariables";
    String deprecatedShowMissingness = showMissingness.equals("FALSE") ? "noVariables" : showMissingness.equals("TRUE") ? "strataVariables" : showMissingness;

    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME,
          spec.getXAxisVariable(),
          spec.getYAxisVariable(),
          util.getVariableSpecFromList(spec.getFacetVariable(), 0),
          util.getVariableSpecFromList(spec.getFacetVariable(), 1)));
      connection.voidEval(getVoidEvalVarMetadataMap(DEFAULT_SINGLE_STREAM_NAME, varMap));
      String cmd = "plot.data::mosaic(" + DEFAULT_SINGLE_STREAM_NAME + ", map, 'bothRatios', '" + deprecatedShowMissingness + "')";
      streamResult(connection, cmd, out);
    });
  }
}
