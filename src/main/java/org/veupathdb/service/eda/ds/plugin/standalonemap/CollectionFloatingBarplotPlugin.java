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

public class CollectionFloatingBarplotPlugin extends AbstractEmptyComputePlugin<CollectionFloatingBarplotPostRequest, CollectionFloatingBarplotSpec> {

  @Override
  public String getDisplayName() {
    return "Bar plot";
  }

  @Override
  public String getDescription() {
    return "Visualize the distribution of a group of categorical variables";
  }

  @Override
  public List<String> getProjects() {
    return List.of(VECTORBASE_PROJECT);
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("xAxisVariable"), List.of("overlayVariable"))
      .pattern()
        .element("xAxisVariable")
          .maxValues(10)
          .description("Variable Group vocabulary must have 10 or fewer unique values.")
        .element("overlayVariable")
      .done();
  }

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new EmptyComputeClassGroup(CollectionFloatingBarplotPostRequest.class, CollectionFloatingBarplotSpec.class);
  }

  @Override
  protected void validateVisualizationSpec(CollectionFloatingBarplotSpec pluginSpec) throws ValidationException {
    ValidationUtils.validateCollectionMembers(getUtil(),
        pluginSpec.getOverlayConfig().getCollection(),
        pluginSpec.getOverlayConfig().getSelectedMembers());
    if (pluginSpec.getBarMode() == null) {
      throw new ValidationException("Property 'barMode' is required.");
    }
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(CollectionFloatingBarplotSpec pluginSpec) {
    String outputEntityId = pluginSpec.getOutputEntityId();
    List<VariableSpec> plotVariableSpecs = new ArrayList<VariableSpec>();
    plotVariableSpecs.addAll(pluginSpec.getOverlayConfig().getSelectedMembers());

    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, outputEntityId)
        .addVars(plotVariableSpecs)
        // TODO can we make this automagical?
        // this will probably need revisited when/ if we introduce study-dependent vocabs for collections
        .addVars(getVariableSpecsWithStudyDependentVocabs(pluginSpec.getOutputEntityId(), plotVariableSpecs)));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    CollectionFloatingBarplotSpec spec = getPluginSpec();
    String barMode = spec.getBarMode().getValue();
    String overlayValues = getRBinListAsString(spec.getOverlayConfig().getSelectedValues());

    Map<String, CollectionSpec> varMap = new HashMap<>();
    varMap.put("overlay", spec.getOverlayConfig().getCollection());
     
    List<DynamicDataSpec> dataSpecsWithStudyDependentVocabs = findCollectionSpecsWithStudyDependentVocabs(varMap);
    Map<String, InputStream> studyVocabs = getVocabByRootEntity(dataSpecsWithStudyDependentVocabs);
    dataStreams.putAll(studyVocabs);

    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(DEFAULT_SINGLE_STREAM_NAME + " <- data.table::fread('" + DEFAULT_SINGLE_STREAM_NAME + "', na.strings=c(''))");
      String inputData = getRCollectionInputDataWithImputedZeroesAsString(DEFAULT_SINGLE_STREAM_NAME, varMap, "variables");
      connection.voidEval(getVoidEvalCollectionMetadataList(varMap));
      String cmd =
          "plot.data::bar(data=" + inputData + ", " +
              "variables=variables, " +
              "value='" + spec.getValueSpec().getValue() + "', " +
              "barmode='" + barMode + "', " +
              "sampleSizes=FALSE, " +
              "completeCases=FALSE, " + 
              "overlayValues=" + overlayValues + ", " + 
              "evilMode='noVariables')";
      streamResult(connection, cmd, out);
    });
  }
}
