package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import org.junit.jupiter.api.Test;

public class ContinuousAggregatorsTest {

  @Test
  public void test() {
    final MarkerAggregator agg = ContinuousAggregators.Median.getAggregatorSupplier().get();
    agg.addValue("5");
    agg.addValue("1");
    agg.addValue("10");
    agg.addValue("2");
    agg.addValue("7");
    System.out.println(agg.finish());
  }

  @Test
  public void test1() {
    final MarkerAggregator agg = ContinuousAggregators.Median.getAggregatorSupplier().get();
    agg.addValue("5");
    agg.addValue("5");
    agg.addValue("1");
    agg.addValue("10");
    agg.addValue("2");
    agg.addValue("7");
    System.out.println(agg.finish());
  }

  @Test
  public void test2() {
    final MarkerAggregator agg = ContinuousAggregators.Median.getAggregatorSupplier().get();
    agg.addValue("4");
    agg.addValue("5");
    agg.addValue("1");
    agg.addValue("10");
    agg.addValue("2");
    agg.addValue("7");
    System.out.println(agg.finish());
  }

  @Test
  public void test3() {
    final MarkerAggregator agg = ContinuousAggregators.Median.getAggregatorSupplier().get();
    agg.addValue("1");
    agg.addValue("2");
    agg.addValue("3");
    agg.addValue("4");
    agg.addValue("5");
    agg.addValue("6");
    System.out.println(agg.finish());
  }

  @Test
  public void test4() {
    final MarkerAggregator agg = ContinuousAggregators.Median.getAggregatorSupplier().get();
    agg.addValue("6");
    agg.addValue("5");
    agg.addValue("4");
    agg.addValue("3");
    agg.addValue("2");
    agg.addValue("1");
    System.out.println(agg.finish());
  }

}
