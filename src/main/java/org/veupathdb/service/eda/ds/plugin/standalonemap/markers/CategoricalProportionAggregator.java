package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

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

  public CategoricalProportionAggregator(Set<String> numeratorValues, Set<String> denominatorValues) {
    this.numeratorValues = numeratorValues;
    this.denominatorValues = denominatorValues;
  }

  @Override
  public void addValue(String s) {
    if (s == null) {
      return;
    }
    if (numeratorValues.contains(s)) {
      numNumeratorMatches++;
    }
    if (denominatorValues.contains(s)) {
      numDenominatorMatches++;
    }
  }

  @Override
  public Double finish() {
    if (numDenominatorMatches == 0) {
      return null;
    }
    return (double) numNumeratorMatches / numDenominatorMatches;
  }
}
