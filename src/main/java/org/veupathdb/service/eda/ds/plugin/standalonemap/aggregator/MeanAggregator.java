package org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator;

import java.util.function.Function;

public class MeanAggregator implements MarkerAggregator {

  private double sum = 0;
  private int n = 0;
  private final int index;
  private final Function<String, Double> variableValueQuantifier;

  public MeanAggregator(int index, Function<String, Double> variableValueQuantifier) {
    this.index = index;
    this.variableValueQuantifier = variableValueQuantifier;
  }

  @Override
  public void addValue(String[] arr) {
    if (arr[index] == null || arr[index].isEmpty()) {
      return;
    }
    sum += variableValueQuantifier.apply(arr[index]);
    n += 1;
  }

  @Override
  public boolean appliesTo(String[] rec) {
    return rec[index] != null && !rec[index].isEmpty();
  }

  @Override
  public Double finish() {
    if (n == 0) {
      return null;
    }
    return sum / n;
  }
}
