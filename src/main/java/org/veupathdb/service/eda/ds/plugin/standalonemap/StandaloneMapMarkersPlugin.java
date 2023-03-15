package org.veupathdb.service.eda.ds.plugin.standalonemap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;

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
import org.veupathdb.service.eda.generated.model.*;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.FormatUtil.TAB;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.CLINEPI_PROJECT;

public class StandaloneMapMarkersPlugin extends AbstractEmptyComputePlugin<StandaloneMapMarkersPostRequest, StandaloneMapMarkersSpec> {

  @Override
  public List<String> getProjects() {
    return List.of(VECTORBASE_PROJECT);
  }
  
  @Override
  protected Class<MapSpec> getVisualizationSpecClass() {
    return MapSpec.class;
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
  protected void validateVisualizationSpec(MapSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("geoAggregateVariable", pluginSpec.getGeoAggregateVariable())
      .var("latitudeVariable", pluginSpec.getLatitudeVariable())
      .var("longitudeVariable", pluginSpec.getLongitudeVariable())
      .var("overlayVariable", pluginSpec.getOverlayVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(MapSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getGeoAggregateVariable())
        .addVar(pluginSpec.getLatitudeVariable())
        .addVar(pluginSpec.getLongitudeVariable())
        .addVar(pluginSpec.getOverlayVariable()));
  }

  private static class MutableInt {
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
      List<BinRangeWithValue> overlayValuesResponse = new List<BinRangeWithValueImpl>();

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
  protected void validateBinRanges(List<BinRange> binRanges) throws ValidationException {
    // all must have starts and ends if any have either
    // ranges must not overlap
  }

  private static String recodeOverlayValue(String oldOverlayValue, List<BinRange> desiredOverlayValues) {
    String newOverlayValue = oldOverlayValue;
    // TODO assuming categorical vars will have labels but not starts and ends defined. maybe thats not explicit enough?
    boolean isContinuous = desiredOverlayValues.stream().filter(bin -> bin.getBinStart() != null).findFirst().isPresent();

    if (isContinuous) {
      newOverlayValue = desiredOverlayValues.stream().filter(bin -> (bin.getBinStart() < oldOverlayValue & bin.getBinEnd() > oldOverlayValue)).findFirst().orElseThrow().getBinLabel();
    } else {
      boolean oldValueIsDesired = desiredOverlayValues.stream().filter(bin -> bin.getBinLabel().equals(oldOverlayValue)).findFirst().isPresent();
      if (!oldValueIsDesired) {
        newOverlayValue = "All un-selected values";
      }
    }

    return(newOverlayValue);
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {

    // create scanner and line parser
    InputStreamReader isReader = new InputStreamReader(new BufferedInputStream(dataStreams.get(DEFAULT_SINGLE_STREAM_NAME)));
    BufferedReader reader = new BufferedReader(isReader);
    DelimitedDataParser parser = new DelimitedDataParser(reader.readLine(), TAB, true);

    // establish column header indexes
    MapSpec spec = getPluginSpec();
    Function<VariableSpec,Integer> indexOf = var -> parser.indexOfColumn(getUtil().toColNameOrEmpty(var)).orElseThrow();
    int geoVarIndex  = indexOf.apply(spec.getGeoAggregateVariable());
    int latIndex     = indexOf.apply(spec.getLatitudeVariable());
    int lonIndex     = indexOf.apply(spec.getLongitudeVariable());
    Integer overlayIndex = spec.getOverlayVariable() == null ? null : indexOf.apply(spec.getOverlayVariable());

    // get map markers config
    List<BinRange> overlayValues = spec.getOverlayValues();
    validateBinRanges(overlayValues);
    String valueSpec = spec.getValueSpec().getValue();

    ParsedGeolocationViewport viewport = ParsedGeolocationViewport.fromApiViewport(spec.getViewport());

    // loop through rows of data stream, aggregating stats into a map from aggregate value to stats object
    Map<String, GeoVarData> aggregator = new HashMap<>();
    String nextLine = reader.readLine();

    while (nextLine != null) {
      String[] row = parser.parseLineToArray(nextLine);
      
      // entity records counts not impacted by viewport
      if (!(row[geoVarIndex] == null ||
            row[geoVarIndex].isEmpty() ||
            row[latIndex] == null ||
            row[latIndex].isEmpty() ||
            row[lonIndex] == null ||
            row[lonIndex].isEmpty())) {
      
        double latitude = Double.parseDouble(row[latIndex]);
        double longitude = Double.parseDouble(row[lonIndex]);
        String overlayValue = overlayIndex == null ? null : recodeOverlayValue(row[overlayIndex], overlayValues);

        if (viewport.containsCoordinates(latitude, longitude)) {
          aggregator.putIfAbsent(row[geoVarIndex], new GeoVarData());
          // overlayValue here could be a raw numeric value as well
          aggregator.get(row[geoVarIndex]).addRow(latitude, longitude, overlayValue);
        }
        nextLine = reader.readLine();
      }  
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
  private static class ParsedGeolocationViewport {
    private double xMin;
    private double xMax;
    private double yMin;
    private double yMax;
    private boolean viewportIncludesIntlDateLine;

    public ParsedGeolocationViewport(double xMin, double xMax,
                                     double yMin, double yMax) {
      this.xMin = xMin;
      this.xMax = xMax;
      this.yMin = yMin;
      this.yMax = yMax;
      this.viewportIncludesIntlDateLine = yMin > yMax;
    }


    public static ParsedGeolocationViewport fromApiViewport(GeolocationViewport viewport) {
      return new ParsedGeolocationViewport(
          Double.parseDouble(viewport.getLatitude().getXMin()),
          Double.parseDouble(viewport.getLatitude().getXMax()),
          viewport.getLongitude().getLeft().doubleValue(),
          viewport.getLongitude().getRight().doubleValue()
      );
    }

    public Boolean containsCoordinates(double latitude, double longitude) {
      if (latitude < xMin || latitude > xMax) { return false; }
      if (viewportIncludesIntlDateLine) {
        if (longitude < yMin && longitude > yMax) { return false; }
      } else {
        if (longitude < yMin || longitude > yMax) { return false; }
      }
      return true;
    }
  }
}
