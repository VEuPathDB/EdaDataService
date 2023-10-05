package org.veupathdb.service.eda.ds.plugin.pass;

import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.core.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.generated.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.veupathdb.service.eda.common.plugin.util.PluginUtil.singleQuote;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.CLINEPI_PROJECT;

public class TwoByTwoPlugin extends AbstractEmptyComputePlugin<TwoByTwoPostRequest, TwoByTwoSpec> {

  @Override
  public String getDisplayName() {
    return "Mosaic plot, 2x2 table";
  }

  @Override
  public String getDescription() {
    return "Visualize the frequency distribution and associated statistics for two dichotomous variables";
  }
  
  @Override
  public List<String> getProjects() {
    return List.of(CLINEPI_PROJECT);
  }

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new EmptyComputeClassGroup(TwoByTwoPostRequest.class, TwoByTwoSpec.class);
  }
  
  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("yAxisVariable", "xAxisVariable"), List.of("facetVariable"))
      .pattern()
        .element("yAxisVariable")
          .minValues(2)
	  .maxValues(2)
	  .description("Variable must have exactly 2 unique values and be from the same branch of the dataset diagram as the X-axis variable.")
        .element("xAxisVariable")
	  .minValues(2)
	  .maxValues(2)
          .description("Variable must have exactly 2 unique values and be from the same branch of the dataset diagram as the Y-axis variable.")
        .element("facetVariable")
          .required(false)
          .maxVars(2)
          .maxValues(10)
          .description("Variable(s) must have 10 or fewer unique values and be of the same or a parent entity as the axes variables.")
      .done();
  }
  
  @Override
  protected void validateVisualizationSpec(TwoByTwoSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("yAxisVariable", pluginSpec.getYAxisVariable())
      .var("facetVariable", pluginSpec.getFacetVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(TwoByTwoSpec pluginSpec) {
    return List.of(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getYAxisVariable())
        .addVars(pluginSpec.getFacetVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    PluginUtil util = getUtil();
    TwoByTwoSpec spec = getPluginSpec();
    Map<String, VariableSpec> varMap = new HashMap<>();
    varMap.put("xAxis", spec.getXAxisVariable());
    varMap.put("yAxis", spec.getYAxisVariable());
    varMap.put("facet1", util.getVariableSpecFromList(spec.getFacetVariable(), 0));
    varMap.put("facet2", util.getVariableSpecFromList(spec.getFacetVariable(), 1));
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "noVariables";
    String deprecatedShowMissingness = showMissingness.equals("FALSE") ? "noVariables" : showMissingness.equals("TRUE") ? "strataVariables" : showMissingness;
    String colRefValue = spec.getXAxisReferenceValue() == null ? "NA_character_" : singleQuote(spec.getXAxisReferenceValue());
    String rowRefValue = spec.getYAxisReferenceValue() == null ? "NA_character_" : singleQuote(spec.getYAxisReferenceValue());

    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME,
          spec.getXAxisVariable(),
          spec.getYAxisVariable(),
          util.getVariableSpecFromList(spec.getFacetVariable(), 0),
          util.getVariableSpecFromList(spec.getFacetVariable(), 1)));
      connection.voidEval(getVoidEvalVariableMetadataList(varMap));
      String cmd = "plot.data::mosaic(data=" + DEFAULT_SINGLE_STREAM_NAME + ", variables=variables, statistic='all', columnReferenceValue=" + 
                                          colRefValue + ", rowReferenceValue=" + 
                                          rowRefValue + ", NULL, TRUE, TRUE, '" + 
                                          deprecatedShowMissingness + "')";
      streamResult(connection, cmd, out);
    });
  }
}
