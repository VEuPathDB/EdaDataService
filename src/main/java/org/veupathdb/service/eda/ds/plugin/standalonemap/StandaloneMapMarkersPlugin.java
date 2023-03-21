package org.veupathdb.service.eda.ds.plugin.standalonemap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.gusdb.fgputil.DelimitedDataParser;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.geo.GeographyUtil.GeographicPoint;
import org.gusdb.fgputil.geo.LatLonAverager;
import org.gusdb.fgputil.validation.ValidationException;
import org.json.JSONObject;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.OverlayBins;
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
    validateOverlayConfig(pluginSpec.getOverlayConfig());
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

  private static class MutableInt { // We can possibly replace with AtomicInteger
    int value = 0;
    public void increment() { ++value;      }
    public void set(int x)  { value = x;    }
    public int  get()       { return value; }
  }

  private static class MutableFloat {
    float value = 0;
    public void set(float x){ value = x;    }
    public float  get()     { return value; }
  }

  private static boolean hasDuplicates(List<String> list) {
    Set<String> seen = new HashSet<>();
    return(list.stream().filter(e -> !seen.add(e)).collect(Collectors.toSet()).size() > 0);
  }

  private static class GeoVarData {

    long count = 0;
    LatLonAverager latLonAvg = new LatLonAverager();
    double minLat = 90;
    double maxLat = -90;
    double minLon = 180;
    double maxLon = -180;
    Map<String, MutableInt> overlayValuesCount = new HashMap<>();
    Map<String, MutableFloat> overlayValuesProportion = new HashMap<>();

    void addRow(double lat, double lon, String overlayValue) {
      count++;
      latLonAvg.addDataPoint(lat, lon);
      minLat = Math.min(minLat, lat);
      minLon = Math.min(minLon, lon);
      maxLat = Math.max(maxLat, lat);
      maxLon = Math.max(maxLon, lon);
      if (overlayValue != null) {
        overlayValuesCount.putIfAbsent(overlayValue, new MutableInt());
        overlayValuesCount.get(overlayValue).increment();
        float proportion = (float)overlayValuesCount.get(overlayValue).get()/count;
        overlayValuesProportion.putIfAbsent(overlayValue, new MutableFloat());
        overlayValuesProportion.get(overlayValue).set(proportion);
      }  
    }

    String getOverlayValues(String valueSpec) {
      List<BinRangeWithValue> overlayValuesResponse = new ArrayList<>();

      for (String overlayValue : overlayValuesCount.keySet()) {
        BinRangeWithValue newValue= new BinRangeWithValueImpl();
        newValue.setBinLabel(overlayValue);
        newValue.setValue(valueSpec.equals("count") ? overlayValuesCount.get(overlayValue) : overlayValuesProportion.get(overlayValue));
        overlayValuesResponse.add(newValue);
      }

      return(overlayValuesResponse);
    }
  }

  // TODO maybe this shoud live somewhere else? idk how generally useful it is
  protected void validateOverlayConfig(OverlayConfig overlayConfig) throws ValidationException {
    // TODO make sure we know how to compare bin start and end. this assumes they are numeric.
    // BUT they probably arent numeric. they might be dates, which means they come in as strings
    // I dont currently know how to differentiate numeric vs date bin ranges here
    // either way they need type conversion...
    switch (overlayConfig.getOverlayType()) {
      case DATE -> validateDateBinRanges((DateOverlayConfig) overlayConfig);
      case NUMBER -> validateNumericBinRanges((NumericOverlayConfig) overlayConfig);
      case CATEGORICAL -> validateCategoricalOverlayValues((CategoricalOverlayConfig) overlayConfig)
    }
  }

  private void validateCategoricalOverlayValues(CategoricalOverlayConfig overlayConfig) throws ValidationException {
    int numDistinctOverlayVals = new HashSet<>(overlayConfig.getOverlayValues()).size();
    if (numDistinctOverlayVals != overlayConfig.getOverlayValues().size()) {
      throw new ValidationException("All overlay values must be unique: " + overlayConfig.getOverlayValues());
    }
  }

  private void validateNumericBinRanges(NumericOverlayConfig overlayConfig) throws ValidationException {
    boolean anyMissingBinStart = overlayConfig.getOverlayValues().stream().anyMatch(bin -> bin.getBinStart() == null);
    boolean anyMissingBinEnd = overlayConfig.getOverlayValues().stream().anyMatch(bin -> bin.getBinEnd() == null);
    if (anyMissingBinStart || anyMissingBinEnd) {
      throw new ValidationException("All numeric bin ranges must have start and end.");
    }
  }

  private void validateDateBinRanges(DateOverlayConfig overlayConfig) throws ValidationException {
    boolean anyMissingBinStart = overlayConfig.getOverlayValues().stream().anyMatch(bin -> bin.getBinStart() == null);
    boolean anyMissingBinEnd = overlayConfig.getOverlayValues().stream().anyMatch(bin -> bin.getBinEnd() == null);
    if (anyMissingBinStart || anyMissingBinEnd) {
      throw new ValidationException("All date bin ranges must have start and end.");
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
    Integer overlayIndex = Optional.of(spec.getOverlayConfig())
        .map(OverlayConfig::getOverlayVariable)
        .map(indexOf)
        .orElse(null);

    // get map markers config
    // TODO update types somehow/ somewhere, need to be able to have numeric or date bin start and ends
    String valueSpec = spec.getValueSpec().getValue();
    OverlayBins overlayBins = new OverlayBins(spec.getOverlayConfig());
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
        String overlayValue = overlayIndex == null ? null : overlayBins.getOverlayRecoder().recode(row[overlayIndex]);

        if (viewport.containsCoordinates(latitude, longitude)) {
          aggregator.putIfAbsent(row[geoVarIndex], new GeoVarData());
          // overlayValue here could be a raw numeric value as well
          aggregator.get(row[geoVarIndex]).addRow(latitude, longitude, overlayValue);
        }
      }
      nextLine = reader.readLine();
    }

    // begin output object and single property containing array of map elements
    out.write("{\"mapElements\":[".getBytes(StandardCharsets.UTF_8));
    boolean first = true;
    for (String key : aggregator.keySet()) {
      // write commas between elements
      if (first) first = false; else out.write(',');
      GeoVarData data = aggregator.get(key);
      GeographicPoint avgLatLon = data.latLonAvg.getCurrentAverage();
      out.write(new JSONObject()
        .put("geoAggregateValue", key)
        .put("entityCount", data.count)
        .put("avgLat", avgLatLon.getLatitude())
        .put("avgLon", avgLatLon.getLongitude())
        .put("minLat", data.minLat)
        .put("minLon", data.minLon)
        .put("maxLat", data.maxLat)
        .put("maxLon", data.maxLon)
        .put("overlayValues", data.getOverlayValues(valueSpec))
        .toString()
        .getBytes(StandardCharsets.UTF_8)
      );
    }
    // close
    String config = "]}";
    out.write(config.getBytes());
    out.flush();
  }
}
