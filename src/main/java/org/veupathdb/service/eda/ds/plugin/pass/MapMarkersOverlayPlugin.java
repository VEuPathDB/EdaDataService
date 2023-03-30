package org.veupathdb.service.eda.ds.plugin.pass;

import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.generated.model.BinSpec;
import org.veupathdb.service.eda.generated.model.MapMarkersOverlayPostRequest;
import org.veupathdb.service.eda.generated.model.MapMarkersOverlaySpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.veupathdb.service.eda.common.plugin.util.PluginUtil.singleQuote;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;

public class MapMarkersOverlayPlugin extends AbstractEmptyComputePlugin<MapMarkersOverlayPostRequest, MapMarkersOverlaySpec> {

  @Override
  public String getDisplayName() {
    return "Map markers";
  }

  @Override
  public String getDescription() {
    return "Visualize counts and proportions of both categorical and continuous data overlaid on map markers.";
  }

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new EmptyComputeClassGroup(MapMarkersOverlayPostRequest.class, MapMarkersOverlaySpec.class);
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("xAxisVariable"), List.of("geoAggregateVariable"))
      .pattern()
        .element("xAxisVariable")
          .maxValues(8)
          .description("Variable must have 8 or fewer unique values.")
        .element("geoAggregateVariable")
          .description("Variable(s) must be of the same or a parent entity of the main variable.")
      .done();
  }

  @Override
  protected void validateVisualizationSpec(MapMarkersOverlaySpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable())
      .var("geoAggregateVariable", pluginSpec.getGeoAggregateVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(MapMarkersOverlaySpec pluginSpec) {
    return List.of(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getXAxisVariable())
        .addVar(pluginSpec.getGeoAggregateVariable())
        .addVar(pluginSpec.getLatitudeVariable())
        .addVar(pluginSpec.getLongitudeVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    PluginUtil util = getUtil();
    MapMarkersOverlaySpec spec = getPluginSpec();
    String showMissingness = spec.getShowMissingness() != null ? spec.getShowMissingness().getValue() : "noVariables";
    String deprecatedShowMissingness = showMissingness.equals("FALSE") ? "noVariables" : showMissingness.equals("TRUE") ? "strataVariables" : showMissingness;
    String valueSpec = singleQuote(spec.getValueSpec().getValue());
    String xVarType = util.getVariableType(spec.getXAxisVariable());

    Map<String, VariableSpec> varMap = new HashMap<>();
    varMap.put("xAxis", spec.getXAxisVariable());
    varMap.put("geo", spec.getGeoAggregateVariable());
    varMap.put("latitude", spec.getLatitudeVariable());
    varMap.put("longitude", spec.getLongitudeVariable());
      
    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME,
          spec.getXAxisVariable(),
          spec.getGeoAggregateVariable(),
          spec.getLatitudeVariable(),
          spec.getLongitudeVariable()));
      connection.voidEval(getVoidEvalVariableMetadataList(varMap));
      String viewportRString = getViewportAsRString(spec.getViewport());
      connection.voidEval(viewportRString);

      String cmd =
          "plot.data::mapMarkers(" + DEFAULT_SINGLE_STREAM_NAME + ", variables, " +
              valueSpec + ", viewport, NULL, TRUE, TRUE, '" +
              deprecatedShowMissingness + "')";
      streamResult(connection, cmd, out);
    });
  }
}
