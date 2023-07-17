package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;


import java.util.function.Function;

/**
 * Marker aggregator for mean/median and standard deviation. Computes online by keeping track of sum, sum of squares and
 * sample count.
 */
public class AveragesWithConfidenceAggregator implements MarkerAggregator<AveragesWithConfidence> {
  private double sum = 0;
  private double sumSquared = 0;
  private int n = 0;
  private final int index;
  private final MarkerAggregator<Double> medianAggregator;
  private final Function<String, Double> variableValueQuantifier;

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
    Double value = variableValueQuantifier.apply(arr[index]);
    // Value can be null if variable value quantifier doesn't produce an applicable result.
    if (value == null) {
      return;
    }
    medianAggregator.addValue(arr);
    sum += value;
    sumSquared += value * value;
    n += 1;
  }

  @Override
  public AveragesWithConfidence finish() {
    final double mean = sum / n;
    final double variance = (sumSquared / n) - Math.pow(mean, 2);
    // Confidence * (stddev / sqrt(n))
    final double confidence = 0.95 * (Math.sqrt(variance) / Math.sqrt(n));
    final double upperBound = mean + confidence;
    final double lowerBound = mean - confidence;
    return new AveragesWithConfidence(
        mean,
        medianAggregator.finish(),
        lowerBound,
        upperBound,
        n
    );
  }

  public int getN() {
    return n;
  }
}
