package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import org.gusdb.fgputil.DelimitedDataParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Component responsible for iterating over data stream and aggregating map marker data per geo variable.
 * @param <T> Type to output as result of aggregation of marker data.
 */
public class MapMarkerRowProcessor<T> {
  private final int geoVarIndex;
  private final int latIndex;
  private final int lonIndex;
  private final Integer overlayIndex;

  public MapMarkerRowProcessor(int geoVarIndex, int latIndex, int lonIndex, Integer overlayIndex) {
    this.geoVarIndex = geoVarIndex;
    this.latIndex = latIndex;
    this.lonIndex = lonIndex;
    this.overlayIndex = overlayIndex;
  }

  public Map<String, MarkerData<T>> process(BufferedReader reader,
                                            DelimitedDataParser parser,
                                            GeolocationViewport viewport,
                                            Supplier<MarkerAggregator<T>> aggregatorSupplier) throws IOException {
    final Map<String, MarkerData<T>> aggregatedDataByGeoVal = new HashMap<>();
    String nextLine = reader.readLine();

    while (nextLine != null) {
      String[] row = parser.parseLineToArray(nextLine);

      // entity records counts not impacted by viewport
      if (!(row[geoVarIndex] == null || row[geoVarIndex].isEmpty() ||
          row[latIndex] == null || row[latIndex].isEmpty() ||
          row[lonIndex] == null || row[lonIndex].isEmpty())) {

        double latitude = Double.parseDouble(row[latIndex]);
        double longitude = Double.parseDouble(row[lonIndex]);

        if (viewport.containsCoordinates(latitude, longitude)) {
          aggregatedDataByGeoVal.putIfAbsent(row[geoVarIndex], new MarkerData<>(aggregatorSupplier.get()));
          // overlayValue here could be a raw numeric value as well
          aggregatedDataByGeoVal.get(row[geoVarIndex]).addRow(latitude, longitude, overlayIndex != null ? row[overlayIndex] : null);
        }
      }
      nextLine = reader.readLine();
    }

    return aggregatedDataByGeoVal;
  }
}
