package org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator;


import java.util.function.Function;

/**
 * Marker aggregator for mean/median and standard deviation. Computes online by keeping track of sum, sum of squares and
 * sample count.
 */
public class MeanWithConfidenceAggregator implements MarkerAggregator<AveragesWithConfidence> {
  private double sum = 0;
  private double sumOfSquares = 0;
  private int n = 0;
  private final int index;
  private final Function<String, Double> variableValueQuantifier;

  public MeanWithConfidenceAggregator(int index, Function<String, Double> variableValueQuantifier) {
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
    sum += value;
    sumOfSquares += value * value;
    n += 1;
  }

  @Override
  public boolean appliesTo(String[] rec) {
    return rec[index] != null && !rec[index].isEmpty();
  }

  @Override
  public AveragesWithConfidence finish() {
    final double mean = sum / n;
    final double sampleVariance = (sumOfSquares - (sum * sum) / n) / (n - 1);
    final double confidence = 1.96 * (Math.sqrt(sampleVariance) / Math.sqrt(n));
    final double upperBound = mean + confidence;
    final double lowerBound = mean - confidence;
    return new AveragesWithConfidence(
        mean,
        lowerBound,
        upperBound,
        n
    );
  }

  public int getN() {
    return n;
  }
}
