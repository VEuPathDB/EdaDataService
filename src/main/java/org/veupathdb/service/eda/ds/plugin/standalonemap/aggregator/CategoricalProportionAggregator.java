package org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Map marker aggregator taking categorical variable and providing a ratio of number of elements with values in a
 * set of numerator values over number of elements with values in a set of denominator values.
 */
public class CategoricalProportionAggregator implements MarkerAggregator<Double> {
  private final boolean negationMode;
  private final int index;
  private final Set<String> numeratorValues;
  private final Set<String> distinctDenominatorValues;

  private int numNumeratorMatches = 0;
  private int distinctDenominatorMatchCount = 0;
  private int numNonNullMatches = 0;

  public CategoricalProportionAggregator(Set<String> numeratorValues,
                                         Set<String> denominatorValues,
                                         Set<String> vocabulary,
                                         int index) {
    if (!denominatorValues.containsAll(numeratorValues)) {
      throw new IllegalArgumentException("Numerator values must be a subset of denominator values.");
    }
    Set<String> numeratorValuesNegation = Sets.difference(vocabulary, numeratorValues);
    // Count one of the following depending on which is cheaper:
    // 1. Total numerator matches.
    // 2. Everything that doesn't match the numerator.
    if (numeratorValuesNegation.size() < numeratorValues.size()) {
      negationMode = true;
      this.numeratorValues = numeratorValuesNegation;
    } else {
      negationMode = false;
      this.numeratorValues = numeratorValues;
    }
    this.distinctDenominatorValues = Sets.difference(denominatorValues, numeratorValues);
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
    if (distinctDenominatorValues.contains(s[index])) {
      distinctDenominatorMatchCount++;
    }
    numNonNullMatches++;
  }

  @Override
  public boolean appliesTo(String[] rec) {
    return rec[index] != null && !rec[index].isEmpty();
  }

  @Override
  public Double finish() {
    int totalNumeratorMatches;
    if (negationMode) {
      totalNumeratorMatches = numNonNullMatches - numNumeratorMatches;
    } else {
      totalNumeratorMatches = numNumeratorMatches;
    }
    final int totalDenominatorMatches = numNumeratorMatches + distinctDenominatorMatchCount;
    if (distinctDenominatorMatchCount == 0) {
      return null;
    }
    return (double) totalNumeratorMatches / totalDenominatorMatches;
  }
}
