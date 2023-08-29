package org.veupathdb.service.eda.ds.plugin.standalonemap;

import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.json.JSONObject;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.OverlaySpecification;
import org.veupathdb.service.eda.generated.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

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
  protected AbstractPlugin<FloatingBarplotPostRequest, FloatingBarplotSpec, Void>.ClassGroup getTypeParameterClasses() {
    return new ClassGroup(FloatingBarplotPostRequest.class, FloatingBarplotSpec.class, Void.class);
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

    Map<String, VariableSpec> varMap = new HashMap<String, VariableSpec>();
    varMap.put("xAxis", spec.getXAxisVariable());
    varMap.put("overlay", overlayVariable);
      
    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME,
          spec.getXAxisVariable(),
          overlayVariable));

      String inputData = getRInputDataWithImputedZeroesAsString(DEFAULT_SINGLE_STREAM_NAME, varMap);
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
