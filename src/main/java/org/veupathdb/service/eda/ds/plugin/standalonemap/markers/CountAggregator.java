package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

public class CountAggregator implements MarkerAggregator<Integer> {
  private int count = 0;

  @Override
  public void addValue(String ignored) {
    count++;
  }

  @Override
  public Integer finish() {
    return count;
  }
}
