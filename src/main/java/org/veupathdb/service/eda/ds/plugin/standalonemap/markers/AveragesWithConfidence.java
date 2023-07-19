package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

/**
 * Data class containing mean, median and confidence interval.
 */
public class AveragesWithConfidence {
  private final double average;
  private final Double intervalLowerBound;
  private final Double intervalUpperBound;
  private final int n;

  public AveragesWithConfidence(double average, Double lowerBound, Double upperBound, int n) {
    this.average = average;
    this.intervalLowerBound = lowerBound;
    this.intervalUpperBound = upperBound;
    this.n = n;
  }

  public double getAverage() {
    return average;
  }

  public Double getIntervalLowerBound() {
    return intervalLowerBound;
  }

  public Double getIntervalUpperBound() {
    return intervalUpperBound;
  }

  public int getN() {
    return n;
  }
}
