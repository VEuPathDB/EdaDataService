package org.veupathdb.service.eda.ds.plugin.standalonemap;

import org.gusdb.fgputil.ListBuilder;
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
import java.util.ArrayList;
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
  protected ClassGroup getTypeParameterClasses() {
    return new EmptyComputeClassGroup(FloatingContTablePostRequest.class, FloatingContTableSpec.class);
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
    String outputEntityId = pluginSpec.getOutputEntityId();
    List<VariableSpec> plotVariableSpecs = new ArrayList<VariableSpec>();
    plotVariableSpecs.add(pluginSpec.getXAxisVariable());
    plotVariableSpecs.add(pluginSpec.getYAxisVariable());

    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, outputEntityId)
        .addVars(plotVariableSpecs)
        // TODO can we make this automagical?
        .addVars(getVariableSpecsWithStudyDependentVocabs(pluginSpec.getOutputEntityId(), plotVariableSpecs)));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    PluginUtil util = getUtil();
    FloatingContTableSpec spec = getPluginSpec();
    Map<String, VariableSpec> varMap = new HashMap<>();
    varMap.put("xAxis", spec.getXAxisVariable());
    varMap.put("yAxis", spec.getYAxisVariable());
   
    List<DynamicDataSpec> dataSpecsWithStudyDependentVocabs = findVariableSpecsWithStudyDependentVocabs(varMap);
    Map<String, InputStream> studyVocabs = getVocabByRootEntity(dataSpecsWithStudyDependentVocabs);
    dataStreams.putAll(studyVocabs);

    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(DEFAULT_SINGLE_STREAM_NAME + " <- data.table::fread('" + DEFAULT_SINGLE_STREAM_NAME + "', na.strings=c(''))");
      String inputData = getRVariableInputDataWithImputedZeroesAsString(DEFAULT_SINGLE_STREAM_NAME, varMap, "variables");
      connection.voidEval(getVoidEvalVariableMetadataList(varMap));
      String cmd = "plot.data::mosaic(data=" + inputData + ", " + 
                                        "variables=variables, " + 
                                        "statistic='chiSq', " + 
                                        "columnReferenceValue=NA_character_, " + 
                                        "rowReferenceValue=NA_character_, "+
                                        "sampleSizes=FALSE, " +
                                        "completeCases=FALSE, " +
                                        "evilMode='noVariables')";
      streamResult(connection, cmd, out);
    });
  }
}
