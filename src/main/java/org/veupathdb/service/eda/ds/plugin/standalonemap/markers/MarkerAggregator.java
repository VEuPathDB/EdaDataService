package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

public interface MarkerAggregator<T> {

  /**
   * Add a variable value to incorporate into aggregated result.
   */
  void addValue(String[] d);

  /**
   * @return result of aggregated variable values.
   */
  T finish();
}
