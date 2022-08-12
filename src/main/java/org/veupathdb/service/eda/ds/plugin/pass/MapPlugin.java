package org.veupathdb.service.eda.ds.plugin.pass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.GeolocationViewport;
import org.veupathdb.service.eda.generated.model.MapPostRequest;
import org.veupathdb.service.eda.generated.model.MapSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.FormatUtil.TAB;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.CLINEPI_PROJECT;

public class MapPlugin extends AbstractPlugin<MapPostRequest, MapSpec> {

  @Override
  public String getDisplayName() {
    return "Geolocation Map";
  }

  @Override
  public String getDescription() {
    return "Visualize available variables on a geographic map.";
  }

  @Override
  public List<String> getProjects() {
    return List.of(CLINEPI_PROJECT);
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
      .done();
  }

  @Override
  protected void validateVisualizationSpec(MapSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("geoAggregateVariable", pluginSpec.getGeoAggregateVariable())
      .var("latitudeVariable", pluginSpec.getLatitudeVariable())
      .var("longitudeVariable", pluginSpec.getLongitudeVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(MapSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVar(pluginSpec.getGeoAggregateVariable())
        .addVar(pluginSpec.getLatitudeVariable())
        .addVar(pluginSpec.getLongitudeVariable()));
  }

  private static class GeoVarData {

    long count = 0;
    LatLonAverager latLonAvg = new LatLonAverager();
    double minLat = 90;
    double maxLat = -90;
    double minLon = 180;
    double maxLon = -180;

    void addRow(double lat, double lon) {
      count++;
      latLonAvg.addDataPoint(lat, lon);
      minLat = Math.min(minLat, lat);
      minLon = Math.min(minLon, lon);
      maxLat = Math.max(maxLat, lat);
      maxLon = Math.max(maxLon, lon);
    }
  }

  private Boolean withinViewport(GeolocationViewport viewport, Double latitude, Double longitude) {
    Double latitudeMin = Double.valueOf(viewport.getLatitude().getXMin());
    Double latitudeMax = Double.valueOf(viewport.getLatitude().getXMax());
    Double longitudeLeft = viewport.getLongitude().getLeft().doubleValue();
    Double longitudeRight = viewport.getLongitude().getRight().doubleValue();
    Boolean viewportIncludesIntlDateLine = longitudeLeft > longitudeRight;

    if (latitude < latitudeMin || latitude > latitudeMax) { return false; }
    if (viewportIncludesIntlDateLine) {
      if (longitude < longitudeLeft && longitude > longitudeRight) { return false; }
    } else {
      if (longitude < longitudeLeft || longitude > longitudeRight) { return false; }
    }

    return true;
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {

    // create scanner and line parser
    Scanner s = new Scanner(dataStreams.get(DEFAULT_SINGLE_STREAM_NAME)).useDelimiter(NL);
    DelimitedDataParser parser = new DelimitedDataParser(s.nextLine(), TAB, true);

    // establish column header indexes
    MapSpec spec = getPluginSpec();
    Function<VariableSpec,Integer> indexOf = var -> parser.indexOfColumn(getUtil().toColNameOrEmpty(var)).orElseThrow();
    int geoVarIndex = indexOf.apply(spec.getGeoAggregateVariable());
    int latIndex    = indexOf.apply(spec.getLatitudeVariable());
    int lonIndex    = indexOf.apply(spec.getLongitudeVariable());

    GeolocationViewport viewport = spec.getViewport();
    long entityRecordsWithGeoVar = 0;

    // loop through rows of data stream, aggregating stats into a map from aggregate value to stats object
    Map<String, GeoVarData> aggregator = new HashMap<>();
    while (s.hasNextLine()) {
      String[] row = parser.parseLineToArray(s.nextLine());
      
      // entity records counts not impacted by viewport
      if (!(row[geoVarIndex] == null ||
	    row[geoVarIndex].equals("") ||
	    row[latIndex] == null ||
	    row[latIndex].equals("") ||
	    row[lonIndex] == null ||
	    row[lonIndex].equals(""))) {
        entityRecordsWithGeoVar++;
      
	Double latitude = Double.valueOf(row[latIndex]);
	Double longitude = Double.valueOf(row[lonIndex]);
        if (withinViewport(viewport, latitude, longitude)) {
          aggregator.putIfAbsent(row[geoVarIndex], new GeoVarData());
          aggregator.get(row[geoVarIndex]).addRow(Double.parseDouble(row[latIndex]), Double.parseDouble(row[lonIndex]));
        }
      }  
    }

    // begin output object and single property containing array of map elements
    out.write("{\"mapElements\":[".getBytes());
    boolean first = true;
    for (String key : aggregator.keySet()) {
      // write commas between elements
      if (first) first = false; else out.write(",".getBytes());
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
        .toString()
        .getBytes()
      );
    }
    // add config and close
    String config = "],\"config\":{\"completeCasesGeoVar\":" + entityRecordsWithGeoVar + "}}";
    out.write(config.getBytes());
    out.flush();
  }
}
