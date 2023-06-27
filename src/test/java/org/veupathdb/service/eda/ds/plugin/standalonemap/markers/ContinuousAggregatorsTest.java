package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ContinuousAggregatorsTest {

  @Test
  @DisplayName("Balanced Sequence, Odd number of elements")
  public void test() {
    final MarkerAggregator<Double> agg = ContinuousAggregators.Median.getAggregatorSupplier().get();
    agg.addValue("5");
    agg.addValue("1");
    agg.addValue("10");
    agg.addValue("2");
    agg.addValue("7");
    Assertions.assertEquals(5.0, agg.finish());
  }

  @Test
  @DisplayName("Balanced Sequence, Even number of elements with dupes")
  public void test1() {
    final MarkerAggregator<Double> agg = ContinuousAggregators.Median.getAggregatorSupplier().get();
    agg.addValue("5");
    agg.addValue("5");
    agg.addValue("1");
    agg.addValue("10");
    agg.addValue("2");
    agg.addValue("7");
    Assertions.assertEquals(5.0, agg.finish());
  }

  @Test
  @DisplayName("Balanced Sequence, Even number of elements no dupes")
  public void test2() {
    final MarkerAggregator<Double> agg = ContinuousAggregators.Median.getAggregatorSupplier().get();
    agg.addValue("4");
    agg.addValue("5");
    agg.addValue("1");
    agg.addValue("10");
    agg.addValue("2");
    agg.addValue("7");
    Assertions.assertEquals(4.5, agg.finish());
  }

  @Test
  @DisplayName("Increasing Sequence, Even number of elements")
  public void test3() {
    final MarkerAggregator<Double> agg = ContinuousAggregators.Median.getAggregatorSupplier().get();
    agg.addValue("1");
    agg.addValue("2");
    agg.addValue("3");
    agg.addValue("4");
    agg.addValue("5");
    agg.addValue("6");
    Assertions.assertEquals(3.5, agg.finish());
  }

  @Test
  @DisplayName("Decreasing Sequence, Even number of elements")
  public void test4() {
    final MarkerAggregator<Double> agg = ContinuousAggregators.Median.getAggregatorSupplier().get();
    agg.addValue("6");
    agg.addValue("5");
    agg.addValue("4");
    agg.addValue("3");
    agg.addValue("2");
    agg.addValue("1");
    Assertions.assertEquals(3.5, agg.finish());
  }

}
