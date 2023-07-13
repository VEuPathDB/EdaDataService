package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;


import java.util.function.Function;

public class AveragesWithConfidenceAggregator implements MarkerAggregator<AveragesWithConfidence> {
  private double sum = 0;
  private double sumSquared = 0;
  private int n = 0;
  private final int index;
  private final MarkerAggregator<Double> medianAggregator;
  private Function<String, Double> variableValueQuantifier;

  public AveragesWithConfidenceAggregator(int index, Function<String, Double> variableValueQuantifier) {
    this.medianAggregator = ContinuousAggregators.Median.getAggregatorSupplier(index);
    this.index = index;
    this.variableValueQuantifier = variableValueQuantifier;
  }

  @Override
  public void addValue(String[] arr) {
    if (arr[index] == null || arr[index].isEmpty()) {
      return;
    }
    medianAggregator.addValue(arr);
    double x = Double.parseDouble(arr[index]);
    sum += x;
    sumSquared += x * x;
    n += 1;
  }

  @Override
  public AveragesWithConfidence finish() {
    final double mean = sum / n;
    final double variance = (sumSquared / n) - Math.pow(mean, 2);
    final double confidence = 0.95 * (Math.sqrt(variance) / Math.sqrt(n));
    final double upperBound = mean + confidence;
    final double lowerBound = mean - confidence;
    return new AveragesWithConfidence(
        mean,
        medianAggregator.finish(),
        lowerBound,
        upperBound
    );
  }
}
