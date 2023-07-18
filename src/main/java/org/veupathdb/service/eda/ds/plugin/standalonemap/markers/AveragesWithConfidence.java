package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

/**
 * Data class containing mean, median and confidence interval.
 */
public class AveragesWithConfidence {
  private final double average;
  private final double intervalLowerBound;
  private final double intervalUpperBound;
  private final int n;

  public AveragesWithConfidence(double average, double lowerBound, double upperBound, int n) {
    this.average = average;
    this.intervalLowerBound = lowerBound;
    this.intervalUpperBound = upperBound;
    this.n = n;
  }

  public double getAverage() {
    return average;
  }

  public double getIntervalLowerBound() {
    return intervalLowerBound;
  }

  public double getIntervalUpperBound() {
    return intervalUpperBound;
  }

  public int getN() {
    return n;
  }
}
