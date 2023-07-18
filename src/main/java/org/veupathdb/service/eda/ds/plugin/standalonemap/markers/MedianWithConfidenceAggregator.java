package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

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
  public AveragesWithConfidence finish() {
    Collections.sort(values);
    // Compute the lower/upper confidence interval:
    // nq +/– z * sqrt( nq(1-q) ) where q is the quantile of interest (0.5 for median)
    double lowerIndex = values.size() * 0.5 - 1.96 * Math.sqrt(values.size() * 0.5 * 0.5);
    double upperIndex = values.size() * 0.5 + 1.96 * Math.sqrt(values.size() * 0.5 * 0.5);
    double lowerBound;
    double upperBound;
    if (lowerIndex < 0) {
      lowerBound = values.get(0);
    } else {
      lowerBound = values.get((int) Math.ceil(lowerIndex));
    }
    if (upperIndex >= values.size()) {
      upperBound = values.get(values.size() - 1);
    } else {
      upperBound = values.get((int) Math.ceil(upperIndex));
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
