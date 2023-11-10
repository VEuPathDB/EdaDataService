package org.veupathdb.service.eda.ds.plugin.standalonemap;

import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.core.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.OverlaySpecification;
import org.veupathdb.service.eda.generated.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class FloatingBarplotPlugin extends AbstractEmptyComputePlugin<FloatingBarplotPostRequest, FloatingBarplotSpec> {
  private OverlaySpecification _overlaySpecification = null;

  @Override
  public String getDisplayName() {
    return "Bar plot";
  }

  @Override
  public String getDescription() {
    return "Visualize the distribution of a categorical variable";
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
          .description("Variable must have 10 or fewer unique values and be of the same or a child entity as the variable the map markers are painted with, if any.")
        .element("overlayVariable")
          .required(false)
      .done();
  }

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new EmptyComputeClassGroup(FloatingBarplotPostRequest.class, FloatingBarplotSpec.class);
  }

  @Override
  protected void validateVisualizationSpec(FloatingBarplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("overlayVariable", Optional.ofNullable(pluginSpec.getOverlayConfig())
          .map(OverlayConfig::getOverlayVariable)
          .orElse(null)));
    if (pluginSpec.getOverlayConfig() != null) {
      try {
        _overlaySpecification = new OverlaySpecification(pluginSpec.getOverlayConfig(), getUtil()::getVariableType, getUtil()::getVariableDataShape);
      } catch (IllegalArgumentException e) {
        throw new ValidationException(e.getMessage());
      }
    }
    if (pluginSpec.getBarMode() == null) {
      throw new ValidationException("Property 'barMode' is required.");
    }
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(FloatingBarplotSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(Optional.ofNullable(pluginSpec.getOverlayConfig())
            .map(OverlayConfig::getOverlayVariable)
            .orElse(null)));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    FloatingBarplotSpec spec = getPluginSpec();
    PluginUtil util = getUtil();
    String barMode = spec.getBarMode().getValue();
    VariableSpec overlayVariable = _overlaySpecification != null ? _overlaySpecification.getOverlayVariable() : null;
    String overlayValues = _overlaySpecification == null ? "NULL" : _overlaySpecification.getRBinListAsString();

    Map<String, VariableSpec> varMap = new HashMap<>();
    varMap.put("xAxis", spec.getXAxisVariable());
    varMap.put("overlay", overlayVariable);

    // TODO can we make this automagical? override useRConnectionWithRemoteFiles ? i wasnt clear how..
    List<DynamicDataSpecImpl> dataSpecsWithStudyDependentVocabs = findVariableSpecsWithStudyDependentVocabs(varMap);
    Map<String, InputStream> studyVocabs = getVocabByRootEntity(dataSpecsWithStudyDependentVocabs);
    dataStreams.putAll(studyVocabs);
 
    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(DEFAULT_SINGLE_STREAM_NAME + " <- data.table::fread('" + DEFAULT_SINGLE_STREAM_NAME + "', na.strings=c(''))");

      String inputData = getRVariableInputDataWithImputedZeroesAsString(DEFAULT_SINGLE_STREAM_NAME, varMap, "variables");
      connection.voidEval(getVoidEvalVariableMetadataList(varMap));
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
