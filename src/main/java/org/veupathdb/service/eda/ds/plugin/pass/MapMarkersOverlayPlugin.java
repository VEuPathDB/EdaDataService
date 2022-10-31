package org.veupathdb.service.eda.ds.plugin.pass;

import org.gusdb.fgputil.ListBuilder;
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
  protected Class<MapMarkersOverlaySpec> getVisualizationSpecClass() {
    return MapMarkersOverlaySpec.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("xAxisVariable"), List.of("geoAggregateVariable"))
      .pattern()
        .element("xAxisVariable")
          .description("Categorical variables with more than 8 values will assign the top 7 values by count to their own categories and assign the additonal values into an 'other' category. Continuous variables will be binned into by default 8 categories.")
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
    return ListBuilder.asList(
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
    varMap.put("xAxisVariable", spec.getXAxisVariable());
    varMap.put("geoAggregateVariable", spec.getGeoAggregateVariable());
    varMap.put("latitudeVariable", spec.getLatitudeVariable());
    varMap.put("longitudeVariable", spec.getLongitudeVariable());
      
    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME,
          spec.getXAxisVariable(),
          spec.getGeoAggregateVariable(),
          spec.getLatitudeVariable(),
          spec.getLongitudeVariable()));
      connection.voidEval(getVoidEvalVarMetadataMap(DEFAULT_SINGLE_STREAM_NAME, varMap));
      String viewportRString = getViewportAsRString(spec.getViewport());
      connection.voidEval(viewportRString);
      String binReportValue = "NULL";
      connection.voidEval("binRange <- NULL");
      if (xVarType.equals("STRING")) {
        // maybe i should have a warning here if they pass a BinSpec?
        connection.voidEval("binWidth <- NULL");
      } else {
        BinSpec binSpec = spec.getBinSpec();
        connection.voidEval("xVals <- " + DEFAULT_SINGLE_STREAM_NAME + "[[map$id[map$plotRef == 'xAxisVariable']]]");
        connection.voidEval("xVals <- xVals[complete.cases(xVals)]");
        connection.voidEval("binWidth <- plot.data::numBinsToBinWidth(xVals, 8)");
        binReportValue = "'binWidth'";
        if (binSpec != null) {
          validateBinSpec(binSpec, xVarType);
          binReportValue = binSpec.getType() != null ? singleQuote(binSpec.getType().getValue()) : binReportValue;
          String binWidth = "NULL";
          String binRangeRString = getBinRangeAsRString(binSpec.getRange());
          connection.voidEval(binRangeRString);
          connection.voidEval("if (is.null(binRange)) { range <- plot.data::findViewport(xVals, " + singleQuote(xVarType) + ") } else { range <- plot.data::validateBinRange(xVals, binRange," + singleQuote(xVarType) + ", FALSE) }");
          connection.voidEval("print(range)");
          connection.voidEval("xVP <- plot.data::adjustToViewport(xVals, range)");
          connection.voidEval("binWidth <- plot.data::numBinsToBinWidth(xVP, 8)");
          if (xVarType.equals("NUMBER") || xVarType.equals("INTEGER")) {
            binWidth = binSpec.getValue() == null ? binWidth : "as.numeric('" + binSpec.getValue() + "')";
          } else {
            binWidth = binSpec.getValue() == null || binSpec.getUnits() == null ? binWidth : "'" + binSpec.getValue().toString() + " " + binSpec.getUnits().toString().toLowerCase() + "'";
          }
          connection.voidEval("binWidth <- " + binWidth);
        }
      }

      String cmd =
          "plot.data::mapMarkers(" + DEFAULT_SINGLE_STREAM_NAME + ", map, binWidth, " +
              valueSpec + ", " +
              binReportValue + ", binRange, viewport, '" +
              deprecatedShowMissingness + "')";
      streamResult(connection, cmd, out);
    });
  }
}
