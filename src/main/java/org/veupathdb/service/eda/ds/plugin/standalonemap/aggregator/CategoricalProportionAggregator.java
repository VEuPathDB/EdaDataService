package org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator;

import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

/**
 * Map marker aggregator taking categorical variable and providing a ratio of number of elements with values in a
 * set of numerator values over number of elements with values in a set of denominator values.
 */
public class CategoricalProportionAggregator implements MarkerAggregator<Double> {
  private static final Logger LOG = LogManager.getLogger(CategoricalProportionAggregator.class);

  private final boolean negationMode;
  private final int index;
  private final Set<String> numeratorMatchSet;
  private final Set<String> distinctDenominatorValues;

  private int numNumeratorMatches = 0;
  private int distinctDenominatorMatchCount = 0;
  private int numNonNullMatches = 0;

  public CategoricalProportionAggregator(Set<String> numeratorMatchSet,
                                         Set<String> distinctDenominatorValues,
                                         boolean negationMode,
                                         int index) {
   this.numeratorMatchSet = numeratorMatchSet;
   this.distinctDenominatorValues = distinctDenominatorValues;
   this.negationMode = negationMode;
    this.index = index;
  }

  @Override
  public void addValue(String[] s) {
    if (s == null) {
      return;
    }
    if (numeratorMatchSet.contains(s[index])) {
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
    final int totalDenominatorMatches = totalNumeratorMatches + distinctDenominatorMatchCount;
    if (totalDenominatorMatches == 0) {
      return null;
    }
    return (double) totalNumeratorMatches / totalDenominatorMatches;
  }
}
