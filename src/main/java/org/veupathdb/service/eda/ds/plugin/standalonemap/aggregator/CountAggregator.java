package org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator;

public class CountAggregator implements MarkerAggregator<Integer> {
  private int count = 0;

  @Override
  public void addValue(String[] ignored) {
    count++;
  }

  @Override
  public boolean appliesTo(String[] rec) {
    return true;
  }

  @Override
  public Integer finish() {
    return count;
  }
}
