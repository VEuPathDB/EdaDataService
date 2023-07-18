package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;


import org.veupathdb.service.eda.ds.utils.StatUtils;

import java.util.List;

/**
 * Marker aggregator for proportions.
 */
public class ProportionWithConfidenceAggregator implements MarkerAggregator<AveragesWithConfidence> {
  private int numeratorMatches = 0;
  private int denominatorMatches = 0;
  private int n = 0;
  private final List<String> numeratorValues;
  private final List<String> denominatorValues;
  private final int index;

  public ProportionWithConfidenceAggregator(int index, List<String> numeratorValues, List<String> denominatorValues) {
    this.index = index;
    this.numeratorValues = numeratorValues;
    this.denominatorValues = denominatorValues;
  }

  @Override
  public void addValue(String[] arr) {
    if (arr[index] == null || arr[index].isEmpty()) {
      return;
    }
    if (numeratorValues.contains(arr[index])) {
      numeratorMatches++;
    }
    if (denominatorValues.contains(arr[index])) {
      n++;
      denominatorMatches++;
    }
  }

  @Override
  public AveragesWithConfidence finish() {
    final double proportion = (double) numeratorMatches / denominatorMatches;
    final double confidence = StatUtils.Z_SCORE_95 * (Math.sqrt((proportion * (1 - proportion)) / n));
    final double upperBound = proportion + confidence;
    final double lowerBound = proportion - confidence;
    return new AveragesWithConfidence(
        proportion,
        lowerBound,
        upperBound,
        n
    );
  }

  public int getN() {
    return n;
  }
}
