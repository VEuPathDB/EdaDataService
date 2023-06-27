package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Overlay aggregator for categorical variables on the standalone map marker.
 */
public class CategoricalOverlayAggregator implements MarkerAggregator<Map<String, CategoricalOverlayAggregator.CategoricalOverlayData>> {
  private final OverlayRecoder overlayRecoder;
  private final Map<String, Integer> count = new HashMap<>();
  private int n = 0;

  public CategoricalOverlayAggregator(OverlayRecoder overlayRecoder) {
    this.overlayRecoder = overlayRecoder;
  }

  @Override
  public void addValue(String d) {
    // Recode the variable from its raw value. This might be quantizing a continuous or a pass-through function for categoricals.
    final String overlayValue = overlayRecoder.recode(d);
    int newCount = count.getOrDefault(overlayValue, 0);
    // Keep track of counts for each overlay var as well as total entity count.
    count.put(overlayValue, newCount + 1);
    n++;
  }

  @Override
  public Map<String, CategoricalOverlayData> finish() {
    return count.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> new CategoricalOverlayData(e.getValue(), (double) e.getValue() / n)));
  }

  public static class CategoricalOverlayData {
    private final int count;
    private final double proportion;

    public CategoricalOverlayData(int count, double proportion) {
      this.count = count;
      this.proportion = proportion;
    }

    public int getCount() {
      return count;
    }

    public double getProportion() {
      return proportion;
    }
  }
}
