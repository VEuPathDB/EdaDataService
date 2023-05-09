package org.veupathdb.service.eda.ds.plugin.standalonemap;

import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.FloatingContTablePostRequest;
import org.veupathdb.service.eda.generated.model.FloatingContTableSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class FloatingContTablePlugin extends AbstractEmptyComputePlugin<FloatingContTablePostRequest, FloatingContTableSpec> {

  @Override
  public String getDisplayName() {
    return "Contingency Table";
  }

  @Override
  public String getDescription() {
    return "Visualize the frequency distribution and associated statistics for two categorical variables";
  }

  @Override
  public List<String> getProjects() {
    return List.of(VECTORBASE_PROJECT);
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("yAxisVariable", "xAxisVariable"))
      .pattern()
        .element("yAxisVariable")
          .maxValues(10)
          .description("Variable must have 10 or fewer unique values and be from the same branch of the dataset diagram as the X-axis variable.")
        .element("xAxisVariable")
          .maxValues(10)
          .description("Variable must have 10 or fewer unique values and be from the same branch of the dataset diagram as the Y-axis variable.")
      .done();
  }

  @Override
  protected AbstractPlugin<FloatingContTablePostRequest, FloatingContTableSpec, Void>.ClassGroup getTypeParameterClasses() {
    return new ClassGroup(FloatingContTablePostRequest.class, FloatingContTableSpec.class, Void.class);
  }

  @Override
  protected void validateVisualizationSpec(FloatingContTableSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("yAxisVariable", pluginSpec.getYAxisVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(FloatingContTableSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getYAxisVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    PluginUtil util = getUtil();
    FloatingContTableSpec spec = getPluginSpec();
    Map<String, VariableSpec> varMap = new HashMap<String, VariableSpec>();
    varMap.put("xAxis", spec.getXAxisVariable());
    varMap.put("yAxis", spec.getYAxisVariable());
    
    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME,
          spec.getXAxisVariable(),
          spec.getYAxisVariable()));
      connection.voidEval(getVoidEvalVariableMetadataList(varMap));
      String cmd = "plot.data::mosaic(data=" + DEFAULT_SINGLE_STREAM_NAME + ", variables=variables, " + 
                                     "statistic='chiSq', " + 
                                     "columnReferenceValue=NA_character_, " + 
                                     "rowReferenceValue=NA_character_, "+
                                     "sampleSizes=FALSE, " +
                                     "completeCases=FALSE, 'noVariables')";
      streamResult(connection, cmd, out);
    });
  }
}
