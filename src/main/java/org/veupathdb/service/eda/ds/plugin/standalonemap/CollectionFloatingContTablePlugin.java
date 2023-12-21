package org.veupathdb.service.eda.ds.plugin.standalonemap;

import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.core.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.ds.utils.ValidationUtils;
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

public class CollectionFloatingContTablePlugin extends AbstractEmptyComputePlugin<CollectionFloatingContTablePostRequest, CollectionFloatingContTableSpec> {

  @Override
  public String getDisplayName() {
    return "Contingency Table";
  }

  @Override
  public String getDescription() {
    return "Visualize the frequency distribution for a Variable Group.";
  }

  @Override
  public List<String> getProjects() {
    return List.of(VECTORBASE_PROJECT);
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("yAxisVariable", "xAxisVariable"));
  }

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new EmptyComputeClassGroup(CollectionFloatingContTablePostRequest.class, CollectionFloatingContTableSpec.class);
  }

  @Override
  protected void validateVisualizationSpec(CollectionFloatingContTableSpec pluginSpec) throws ValidationException {
    ValidationUtils.validateCollectionMembers(getUtil(),
        pluginSpec.getXAxisVariable().getCollection(),
        pluginSpec.getXAxisVariable().getSelectedMembers());
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(CollectionFloatingContTableSpec pluginSpec) {   
    String outputEntityId = pluginSpec.getOutputEntityId();
    List<VariableSpec> plotVariableSpecs = new ArrayList<VariableSpec>();
    plotVariableSpecs.addAll(pluginSpec.getXAxisVariable().getSelectedMembers());

    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, outputEntityId)
        .addVars(plotVariableSpecs)
        // TODO can we make this automagical?
        .addVars(getVariableSpecsWithStudyDependentVocabs(pluginSpec.getOutputEntityId(), plotVariableSpecs))); 
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    CollectionFloatingContTableSpec spec = getPluginSpec();
    String outputEntityId = spec.getOutputEntityId();
    String overlayValues = getRBinListAsString(spec.getXAxisVariable().getSelectedValues());
    Map<String, CollectionSpec> varMap = new HashMap<>();
    varMap.put("xAxis", spec.getXAxisVariable().getCollection());
   
    List<DynamicDataSpec> dataSpecsWithStudyDependentVocabs = getDynamicDataSpecsWithStudyDependentVocabs(outputEntityId);
    Map<String, InputStream> studyVocabs = getVocabByRootEntity(dataSpecsWithStudyDependentVocabs);
    dataStreams.putAll(studyVocabs);

    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(DEFAULT_SINGLE_STREAM_NAME + " <- data.table::fread('" + DEFAULT_SINGLE_STREAM_NAME + "', na.strings=c(''))");
      String inputData = getRCollectionInputDataWithImputedZeroesAsString(DEFAULT_SINGLE_STREAM_NAME, varMap, outputEntityId, "variables");
      connection.voidEval(getVoidEvalCollectionMetadataList(varMap));
      String cmd = "plot.data::mosaic(data=" + inputData + ", " + 
                                        "variables=variables, " + 
                                        "statistic='chiSq', " + 
                                        "columnReferenceValue=NA_character_, " + 
                                        "rowReferenceValue=NA_character_, "+
                                        "sampleSizes=FALSE, " +
                                        "completeCases=FALSE, " +
                                        "overlayValues=" + overlayValues + ", " +
                                        "evilMode='noVariables')";
      streamResult(connection, cmd, out);
    });
  }
}
