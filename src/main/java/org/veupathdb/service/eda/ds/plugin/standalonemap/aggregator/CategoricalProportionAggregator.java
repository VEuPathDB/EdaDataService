package org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator;

import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.veupathdb.service.eda.generated.model.CategoricalAggregationConfig;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

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

  private CategoricalProportionAggregator(Set<String> numeratorMatchSet,
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

    // Negation mode means we're counting every row that does NOT match the numerator for efficiency.
    if (negationMode) {
      totalNumeratorMatches = numNonNullMatches - numNumeratorMatches;
    } else {
      totalNumeratorMatches = numNumeratorMatches;
    }

    // Since numerator is a subset of denominator, total denominator matches is numerator + distinct denominator matches.
    final int totalDenominatorMatches = totalNumeratorMatches + distinctDenominatorMatchCount;
    if (totalDenominatorMatches == 0) {
      return null;
    }
    return (double) totalNumeratorMatches / totalDenominatorMatches;
  }

  public static class CategoricalProportionAggregatorFactory implements ContinuousAggregators.ContinuousAggregatorFactory {
    private final CategoricalAggregationConfig categoricalConfig;
    private final List<String> vocab;
    private final Set<String> distinctDenominatorValues;
    private final Set<String> numeratorMatchSet;
    private final boolean negationMode;

    /**
     * Constructs a CategoricalProportionAggregatorFactory. This factory abstracts the constructor of the
     * CategoricalProportionAggregator class to avoid re-computing negation match set and distinct denominator values
     * for each instance of CategoricalProportionAggregator for a given request.
     *
     * @param categoricalConfig The user-specified aggregation config for the request.
     * @param vocabSupplier A supplier for the vocabulary of the variable of interest.
     */
    public CategoricalProportionAggregatorFactory(CategoricalAggregationConfig categoricalConfig,
                                                  Supplier<List<String>> vocabSupplier) {
      if (!categoricalConfig.getDenominatorValues().containsAll(categoricalConfig.getNumeratorValues())) {
        throw new IllegalArgumentException("Numerator values match set must be a subset of denominator values match set.");
      }

      Set<String> numeratorMatchSet = new HashSet<>(categoricalConfig.getNumeratorValues());
      Set<String> vocabulary = new HashSet<>(vocabSupplier.get());
      Set<String> numeratorNegationMatchSet = Sets.difference(vocabulary, numeratorMatchSet);

      // Count one of the following depending on which is cheaper:
      // 1. Total numerator matches.
      // 2. Everything that doesn't match the numerator.
      if (numeratorNegationMatchSet.size() < numeratorMatchSet.size()) {
        this.negationMode = true;
        this.numeratorMatchSet = numeratorNegationMatchSet;
      } else {
        this.negationMode = false;
        this.numeratorMatchSet = numeratorMatchSet;
      }

      // Since numerator values are a subset of denominator values, we know anything in the numerator set is always
      // in the denominator. We can just check the distinct denominator values and add back in the numerator matches.
      Set<String> denominatorValues = new HashSet<>(categoricalConfig.getDenominatorValues());
      this.distinctDenominatorValues = Sets.difference(denominatorValues, numeratorMatchSet);
      this.categoricalConfig = categoricalConfig;
      this.vocab = vocabSupplier.get();
    }

    @Override
    public MarkerAggregator<Double> create(int index, Function<String, Double> valueQuantifier) {
      return new CategoricalProportionAggregator(numeratorMatchSet,
          distinctDenominatorValues,
          negationMode,
          index);
    }

    @Override
    public MarkerAggregator<AveragesWithConfidence> createWithConfidence(int index, Function<String, Double> valueQuantifier) {
      return new ProportionWithConfidenceAggregator(index,
          categoricalConfig.getNumeratorValues(),
          categoricalConfig.getDenominatorValues(),
          vocab);
    }
  }
}
