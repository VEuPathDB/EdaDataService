package org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Marker aggregator that keeps track of all values in-memory in order to compute the median and confidence interval.
 *
 * Note that this will be memory inefficient for large datasets. We may want to give an option to approximate
 * median for large datasets to avoid loading too much into memory.
 */
public class MedianWithConfidenceAggregator implements MarkerAggregator<AveragesWithConfidence> {

  private final List<Double> values = new ArrayList<>();
  private final int index;

  public MedianWithConfidenceAggregator(int index) {
    this.index = index;
  }

  @Override
  public void addValue(String[] arr) {
    if (arr[index] == null || arr[index].isEmpty()) {
      return;
    }
    values.add(Double.parseDouble(arr[index]));
  }

  @Override
  public boolean appliesTo(String[] rec) {
    return rec[index] != null && !rec[index].isEmpty();
  }

  @Override
  public AveragesWithConfidence finish() {
    Collections.sort(values);
    // Compute the lower/upper confidence interval:
    // nq +/– z * sqrt( nq(1-q) ) where q is the quantile of interest (0.5 for median)
    double lowerIndex = values.size() * 0.5 - 1.96 * Math.sqrt(values.size() * 0.5 * 0.5);
    double upperIndex = values.size() * 0.5 + 1.96 * Math.sqrt(values.size() * 0.5 * 0.5);
    Double lowerBound;
    Double upperBound;
    if (lowerIndex < 0 || upperIndex >= values.size()) {
      upperBound = null;
      lowerBound = null;
    } else {
      upperBound = values.get((int) Math.ceil(upperIndex));
      lowerBound = values.get((int) Math.ceil(lowerIndex));
    }
    double medianIndex = values.size() / 2.0;
    int lowerMedianIndex = (int) Math.ceil(medianIndex);
    int upperMedianIndex = (int) Math.floor(medianIndex);
    if (lowerMedianIndex == upperMedianIndex) {
      return new AveragesWithConfidence(values.get(upperMedianIndex), lowerBound, upperBound, values.size());
    } else {
      return new AveragesWithConfidence(values.get(upperMedianIndex) + values.get(lowerMedianIndex) / 2.0,
          lowerBound, upperBound, values.size());
    }
  }
}
