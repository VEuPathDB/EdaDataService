package org.veupathdb.service.eda.ds.plugin.standalonemap;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.gusdb.fgputil.DelimitedDataParser;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.geo.GeographyUtil.GeographicPoint;
import org.gusdb.fgputil.geo.LatLonAverager;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.OverlaySpecification;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.GeolocationViewport;
import org.veupathdb.service.eda.generated.model.*;

import static org.gusdb.fgputil.FormatUtil.TAB;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class StandaloneMapMarkersPlugin extends AbstractEmptyComputePlugin<StandaloneMapMarkersPostRequest, StandaloneMapMarkersSpec> {

  @Override
  public List<String> getProjects() {
    return List.of(VECTORBASE_PROJECT);
  }

  @Override
  protected Class<StandaloneMapMarkersPostRequest> getVisualizationRequestClass() {
    return StandaloneMapMarkersPostRequest.class;
  }

  @Override
  protected Class<StandaloneMapMarkersSpec> getVisualizationSpecClass() {
    return StandaloneMapMarkersSpec.class;
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .pattern()
        .element("geoAggregateVariable")
          .types(APIVariableType.STRING)
        .element("latitudeVariable")
          .types(APIVariableType.NUMBER)
        .element("longitudeVariable")
          .types(APIVariableType.LONGITUDE)
        .element("overlayVariable")
      .done();
  }

  @Override
  protected void validateVisualizationSpec(StandaloneMapMarkersSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("geoAggregateVariable", pluginSpec.getGeoAggregateVariable())
      .var("latitudeVariable", pluginSpec.getLatitudeVariable())
      .var("longitudeVariable", pluginSpec.getLongitudeVariable())
      .var("overlayVariable", Optional.ofNullable(pluginSpec.getOverlayConfig())
          .map(OverlayConfig::getOverlayVariable)
          .orElse(null)));
    if (pluginSpec.getOverlayConfig() != null) {
      validateOverlayConfig(pluginSpec.getOverlayConfig());
    }
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(StandaloneMapMarkersSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getGeoAggregateVariable())
        .addVar(pluginSpec.getLatitudeVariable())
        .addVar(pluginSpec.getLongitudeVariable())
        .addVar(Optional.ofNullable(pluginSpec.getOverlayConfig())
            .map(OverlayConfig::getOverlayVariable)
            .orElse(null)));
  }

  private static class GeoVarData {

    long count = 0;
    LatLonAverager latLonAvg = new LatLonAverager();
    double minLat = 90;
    double maxLat = -90;
    double minLon = 180;
    double maxLon = -180;
    Map<String, AtomicInteger> overlayValuesCount = new HashMap<>();
    Map<String, Float> overlayValuesProportion = new HashMap<>();

    void addRow(double lat, double lon, String overlayValue) {
      count++;
      latLonAvg.addDataPoint(lat, lon);
      minLat = Math.min(minLat, lat);
      minLon = Math.min(minLon, lon);
      maxLat = Math.max(maxLat, lat);
      maxLon = Math.max(maxLon, lon);
      if (overlayValue != null) {
        overlayValuesCount.putIfAbsent(overlayValue, new AtomicInteger(0));
        overlayValuesCount.get(overlayValue).incrementAndGet();
        float proportion = (float) overlayValuesCount.get(overlayValue).get() / count;
        overlayValuesProportion.put(overlayValue, proportion);
      }  
    }

    List<BinRangeWithValue> getOverlayValues(String valueSpec) {
      List<BinRangeWithValue> overlayValuesResponse = new ArrayList<>();

      for (String overlayValue : overlayValuesCount.keySet()) {
        BinRangeWithValue newValue = new BinRangeWithValueImpl();
        newValue.setBinLabel(overlayValue);
        newValue.setValue(valueSpec.equals("count") ? overlayValuesCount.get(overlayValue).get() : overlayValuesProportion.get(overlayValue));
        overlayValuesResponse.add(newValue);
      }

      return(overlayValuesResponse);
    }
  }

  protected void validateOverlayConfig(OverlayConfig overlayConfig) throws ValidationException {
    switch (overlayConfig.getOverlayType()) {
      case CONTINOUS -> validateContinousBinRanges((ContinousOverlayConfig) overlayConfig);
      case CATEGORICAL -> validateCategoricalOverlayValues((CategoricalOverlayConfig) overlayConfig);
    }
  }

  private void validateCategoricalOverlayValues(CategoricalOverlayConfig overlayConfig) throws ValidationException {
    if (!getUtil().getVariableDataShape(overlayConfig.getOverlayVariable()).equalsIgnoreCase(APIVariableDataShape.CATEGORICAL.toString())) {
      throw new ValidationException("Input overlay variable %s is %s, but provided overlay configuration is for a categorical variable"
          .formatted(overlayConfig.getOverlayVariable().getVariableId(), getUtil().getVariableDataShape(overlayConfig.getOverlayVariable())));
    }
    int numDistinctOverlayVals = new HashSet<>(overlayConfig.getOverlayValues()).size();
    if (numDistinctOverlayVals != overlayConfig.getOverlayValues().size()) {
      throw new ValidationException("All overlay values must be unique: " + overlayConfig.getOverlayValues());
    }
  }

  private void validateContinousBinRanges(ContinousOverlayConfig overlayConfig) throws ValidationException {
    if (!getUtil().getVariableDataShape(overlayConfig.getOverlayVariable()).equalsIgnoreCase(APIVariableDataShape.CONTINUOUS.toString())) {
      throw new ValidationException("Input overlay variable %s is %s, but provided overlay configuration is for a conginuous variable"
          .formatted(overlayConfig.getOverlayVariable().getVariableId(), getUtil().getVariableDataShape(overlayConfig.getOverlayVariable())));
    }
    boolean anyMissingBinStart = overlayConfig.getOverlayValues().stream().anyMatch(bin -> bin.getStart() == null);
    boolean anyMissingBinEnd = overlayConfig.getOverlayValues().stream().anyMatch(bin -> bin.getEnd() == null);
    if (anyMissingBinStart || anyMissingBinEnd) {
      throw new ValidationException("All numeric bin ranges must have start and end.");
    }
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {

    // create scanner and line parser
    InputStreamReader isReader = new InputStreamReader(new BufferedInputStream(dataStreams.get(DEFAULT_SINGLE_STREAM_NAME)));
    BufferedReader reader = new BufferedReader(isReader);
    DelimitedDataParser parser = new DelimitedDataParser(reader.readLine(), TAB, true);

    // establish column header indexes
    StandaloneMapMarkersSpec spec = getPluginSpec();
    Function<VariableSpec,Integer> indexOf = var -> parser.indexOfColumn(getUtil().toColNameOrEmpty(var)).orElseThrow();
    int geoVarIndex  = indexOf.apply(spec.getGeoAggregateVariable());
    int latIndex     = indexOf.apply(spec.getLatitudeVariable());
    int lonIndex     = indexOf.apply(spec.getLongitudeVariable());
    Optional<OverlayConfig> overlayConfig = Optional.ofNullable(spec.getOverlayConfig());
    Integer overlayIndex = overlayConfig
        .map(OverlayConfig::getOverlayVariable)
        .map(indexOf)
        .orElse(null);

    // get map markers config
    // TODO update types somehow/ somewhere, need to be able to have numeric or date bin start and ends
    String valueSpec = spec.getValueSpec().getValue();
    Optional<OverlaySpecification> overlaySpecification = overlayConfig.map(config -> new OverlaySpecification(config, getUtil()::getVariableType));
    GeolocationViewport viewport = GeolocationViewport.fromApiViewport(spec.getViewport());

    // loop through rows of data stream, aggregating stats into a map from aggregate value to stats object
    Map<String, GeoVarData> aggregator = new HashMap<>();
    String nextLine = reader.readLine();

    while (nextLine != null) {
      String[] row = parser.parseLineToArray(nextLine);
      
      // entity records counts not impacted by viewport
      if (!(row[geoVarIndex] == null || row[geoVarIndex].isEmpty() ||
            row[latIndex] == null || row[latIndex].isEmpty() ||
            row[lonIndex] == null || row[lonIndex].isEmpty())) {
      
        double latitude = Double.parseDouble(row[latIndex]);
        double longitude = Double.parseDouble(row[lonIndex]);
        String overlayValue = overlaySpecification
            .map(overlaySpec -> overlaySpec.recode(row[overlayIndex]))
            .orElse(null);

        if (viewport.containsCoordinates(latitude, longitude)) {
          aggregator.putIfAbsent(row[geoVarIndex], new GeoVarData());
          // overlayValue here could be a raw numeric value as well
          aggregator.get(row[geoVarIndex]).addRow(latitude, longitude, overlayValue);
        }
      }
      nextLine = reader.readLine();
    }

    List<StandaloneMapElementInfo> output = new ArrayList<>();
    for (String key : aggregator.keySet()) {
      StandaloneMapElementInfo mapEle = new StandaloneMapElementInfoImpl();
      GeoVarData data = aggregator.get(key);
      GeographicPoint avgLatLon = data.latLonAvg.getCurrentAverage();
      mapEle.setAvgLat(avgLatLon.getLatitude());
      mapEle.setAvgLon(avgLatLon.getLongitude());
      mapEle.setMinLat(data.minLat);
      mapEle.setMaxLat(data.maxLat);
      mapEle.setMinLon(data.minLon);
      mapEle.setMaxLon(data.maxLon);
      mapEle.setOverlayValues(data.getOverlayValues(valueSpec));
      output.add(mapEle);
    }
    StandaloneMapMarkersPostResponse response = new StandaloneMapMarkersPostResponseImpl();
    response.setMapElements(output);
    JsonUtil.Jackson.writeValue(out, response);
    out.flush();
  }
}
