package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

public class AveragesWithConfidence {
  private double mean;
  private double median;
  private double intervalLowerBound;
  private double intervalUpperBound;

  public AveragesWithConfidence(double mean, double median, double lowerBound, double upperBound) {
    this.mean = mean;
    this.median = median;
    this.intervalLowerBound = lowerBound;
    this.intervalUpperBound = upperBound;
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
}
