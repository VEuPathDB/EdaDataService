package org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator;

public interface MarkerAggregator<T> {

  /**
   * Add a variable value to incorporate into aggregated result.
   */
  void addValue(String[] rec);

  /**
   * Returns true if
   * @param rec
   * @return
   */
  boolean appliesTo(String[] rec);

  /**
   * @return result of aggregated variable values.
   */
  T finish();
}
