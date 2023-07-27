package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import org.gusdb.fgputil.geo.LatLonAverager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MarkerData<T> {

  long count = 0;
  LatLonAverager latLonAvg = new LatLonAverager();
  double minLat = 90;
  double maxLat = -90;
  double minLon = 180;
  double maxLon = -180;
  MarkerAggregator<T> markerAggregator;

  public MarkerData(MarkerAggregator<T> markerAggregator) {
    this.markerAggregator = markerAggregator;
  }

  public LatLonAverager getLatLonAvg() {
    return latLonAvg;
  }

  public double getMinLat() {
    return minLat;
  }

  public double getMaxLat() {
    return maxLat;
  }

  public double getMinLon() {
    return minLon;
  }

  public double getMaxLon() {
    return maxLon;
  }

  public long getCount() {
    return count;
  }

  public MarkerAggregator<T> getMarkerAggregator() {
    return markerAggregator;
  }

  public void addRow(double lat, double lon, String[] row) {
    count++;
    latLonAvg.addDataPoint(lat, lon);
    minLat = Math.min(minLat, lat);
    minLon = Math.min(minLon, lon);
    maxLat = Math.max(maxLat, lat);
    maxLon = Math.max(maxLon, lon);
    if (markerAggregator != null) {
      markerAggregator.addValue(row);
    }
  }
}
