package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

/**
 * Data class containing mean, median and confidence interval.
 */
public class AveragesWithConfidence {
  private final double mean;
  private final double median;
  private final double intervalLowerBound;
  private final double intervalUpperBound;
  private final int n;

  public AveragesWithConfidence(double mean, double median, double lowerBound, double upperBound, int n) {
    this.mean = mean;
    this.median = median;
    this.intervalLowerBound = lowerBound;
    this.intervalUpperBound = upperBound;
    this.n = n;
  }

  public double getMean() {
    return mean;
  }

  public double getMedian() {
    return median;
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
