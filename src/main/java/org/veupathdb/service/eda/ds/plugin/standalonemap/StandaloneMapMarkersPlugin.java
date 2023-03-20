package org.veupathdb.service.eda.ds.plugin.standalonemap;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
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
    // TODO make sure we know how to compare bin start and end. this assumes they are numeric.
    // BUT they probably arent numeric. they might be dates, which means they come in as strings
    // I dont currently know how to differentiate numeric vs date bin ranges here
    // either way they need type conversion...

    // maybe there is a better way to do this type of thing??
    boolean anyHaveBinStart = binRanges.stream().filter(bin -> bin.getBinStart() != null).findFirst().isPresent();
    boolean anyHaveBinEnd = binRanges.stream().filter(bin -> bin.getBinEnd() != null).findFirst().isPresent();
    boolean allHaveBinStart = !binRanges.stream().filter(bin -> bin.getBinStart() == null).findFirst().isPresent();
    boolean allHaveBinEnd = !binRanges.stream().filter(bin -> bin.getBinEnd() == null).findFirst().isPresent();
    if ((anyHaveBinStart | anyHaveBinEnd) & !(allHaveBinStart & allHaveBinEnd)) {
      throw new ValidationException("All BinRanges must have binStart and binEnd if any BinRange has either.");
    }

    // start is always before end
    boolean anyStartAfterEnd = binRanges.stream().filter(bin -> bin.getBinEnd() <= bin.getBinStart()).findFirst().isPresent();
    if (anyHaveBinStart & anyStartAfterEnd) {
      throw new ValidationException("All BinRanges must have binStart less than binEnd.");
    }

    // no duplicate labels
    List<String> labels = new ArrayList<String>();
    binRanges.forEach( bin -> labels.add(bin.getBinLabel()) );
    if (hasDuplicates(labels)) {
      throw new ValidationException("All BinRanges must have unique binLabels.");
    }    

    // ranges must not overlap
    // TODO again need to add support for dates here
    if (anyHaveBinStart) {
      Map<Double, Double> binEdges = new TreeMap<Double, Double>();
      binRanges.forEach( bin -> binEdges.put(Double.parseDouble(bin.getBinStart()), Double.parseDouble(bin.getBinEnd())));
      boolean first = true;
      for (Double binStart : binEdges.keySet()) {
        Double prevBinEnd;
        if (first) {
          prevBinEnd = binEdges.get(binStart);
          first = false;
        } else {
          if (prevBinEnd > binStart) {
            throw new ValidationException("Bin Ranges must not overlap");
          }
          prevBinEnd = binEdges.get(binStart);
        }
      }
    }
  }

  private static String recodeOverlayValue(String oldOverlayValue, List<BinRange> desiredOverlayValues) {
    String newOverlayValue = oldOverlayValue;
    // TODO assuming categorical vars will have labels but not starts and ends defined. maybe thats not explicit enough?
    boolean isContinuous = desiredOverlayValues.stream().filter(bin -> bin.getBinStart() != null).findFirst().isPresent();

    if (isContinuous) {
      // TODO figure something else out for dates- not sure yet how to compare dates in java
      newOverlayValue = desiredOverlayValues.stream().filter(bin -> (Double.parseDouble(bin.getBinStart()) < Double.parseDouble(oldOverlayValue) & Double.parseDouble(bin.getBinEnd()) > Double.parseDouble(oldOverlayValue))).findFirst().orElseThrow().getBinLabel();
    } else {
      boolean oldValueIsDesired = desiredOverlayValues.stream().filter(bin -> bin.getBinLabel().equals(oldOverlayValue)).findFirst().isPresent();
      if (!oldValueIsDesired) {
        newOverlayValue = "__UNSELECTED__";
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
    // TODO update types somehow/ somewhere, need to be able to have numeric or date bin start and ends
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
