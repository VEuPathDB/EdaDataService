package org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator;

import java.util.Set;

/**
 * Map marker aggregator taking categorical variable and providing a ratio of number of elements with values in a
 * set of numerator values over number of elements with values in a set of denominator values.
 */
public class CategoricalProportionAggregator implements MarkerAggregator<Double> {
  private final Set<String> numeratorValues;
  private final Set<String> denominatorValues;
  private int numNumeratorMatches = 0;
  private int numDenominatorMatches = 0;
  private int index;

  public CategoricalProportionAggregator(Set<String> numeratorValues, Set<String> denominatorValues, int index) {
    if (!denominatorValues.containsAll(numeratorValues)) {
      throw new IllegalArgumentException("Numerator values must be a subset of denominator values.");
    }
    this.numeratorValues = numeratorValues;
    this.denominatorValues = denominatorValues;
    this.index = index;
  }

  @Override
  public void addValue(String[] s) {
    if (s == null) {
      return;
    }
    if (numeratorValues.contains(s[index])) {
      numNumeratorMatches++;
    }
    if (denominatorValues.contains(s[index])) {
      numDenominatorMatches++;
    }
  }

  @Override
  public boolean appliesTo(String[] rec) {
    return rec[index] != null && !rec[index].isEmpty();
  }

  @Override
  public Double finish() {
    if (numDenominatorMatches == 0) {
      return null;
    }
    return (double) numNumeratorMatches / numDenominatorMatches;
  }
}
