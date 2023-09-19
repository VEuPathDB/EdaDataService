package org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator;


import org.veupathdb.service.eda.ds.utils.StatUtils;

import java.util.List;

/**
 * Marker aggregator for proportions.
 */
public class ProportionWithConfidenceAggregator implements MarkerAggregator<AveragesWithConfidence> {
  private int numeratorMatches = 0;
  private int denominatorMatches = 0;
  private int n = 0;
  private final List<String> numeratorValues;
  private final List<String> denominatorValues;
  private final int index;

  private final boolean numeratorAlwaysOne;
  private final boolean denominatorAlwaysOne;

  public ProportionWithConfidenceAggregator(int index,
                                            List<String> numeratorValues,
                                            List<String> denominatorValues,
                                            List<String> overlayVocab) {
    this.numeratorAlwaysOne = numeratorValues.containsAll(overlayVocab);
    this.denominatorAlwaysOne = denominatorValues.containsAll(overlayVocab);
    this.index = index;
    this.numeratorValues = numeratorValues;
    this.denominatorValues = denominatorValues;
  }

  @Override
  public void addValue(String[] arr) {
    if (arr[index] == null || arr[index].isEmpty()) {
      return;
    }
    if (numeratorAlwaysOne || numeratorValues.contains(arr[index])) {
      numeratorMatches++;
      denominatorMatches++;
      n++;
      return;
    }

    if (denominatorAlwaysOne || denominatorValues.contains(arr[index])) {
      n++;
      denominatorMatches++;
    }
  }

  @Override
  public AveragesWithConfidence finish() {
    final double proportion = (double) numeratorMatches / denominatorMatches;
    final double confidence = StatUtils.Z_SCORE_95 * (Math.sqrt((proportion * (1 - proportion)) / n));
    final double upperBound = Math.min(1.0, proportion + confidence);
    final double lowerBound = Math.max(0.0, proportion - confidence);
    return new AveragesWithConfidence(
        proportion,
        lowerBound,
        upperBound,
        n
    );
  }

  @Override
  public boolean appliesTo(String[] rec) {
    return rec[index] != null && !rec[index].isEmpty();
  }

  public int getN() {
    return n;
  }
}
