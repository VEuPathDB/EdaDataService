package org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.veupathdb.service.eda.generated.model.CategoricalAggregationConfig;
import org.veupathdb.service.eda.generated.model.CategoricalAggregationConfigImpl;

import java.util.Set;

public class CategoricalProportionAggregatorTest {

  @Test
  @DisplayName("Numerator small proportion of vocab.")
  public void test1() {
    final MarkerAggregator<Double> agg = createClassUnderTest(Set.of("1"),
        Set.of("1", "2", "3"),
        Set.of("1", "2", "3", "4", "5", "6"),
        0);
    agg.addValue(new String[] { "6" });
    agg.addValue(new String[] { "5" });
    agg.addValue(new String[] { "4" });
    agg.addValue(new String[] { "3" });
    agg.addValue(new String[] { "2" });
    agg.addValue(new String[] { "1" });
    Assertions.assertEquals(0.333, agg.finish(), 0.001);
  }

  @Test
  @DisplayName("Numerator small proportion of vocab. Denominator all of vocab.")
  public void test2() {
    final MarkerAggregator<Double> agg = createClassUnderTest(Set.of("1"),
        Set.of("1", "2", "3", "4", "5", "6"),
        Set.of("1", "2", "3", "4", "5", "6"),
        0);
    agg.addValue(new String[] { "6" });
    agg.addValue(new String[] { "5" });
    agg.addValue(new String[] { "4" });
    agg.addValue(new String[] { "3" });
    agg.addValue(new String[] { "2" });
    agg.addValue(new String[] { "1" });
    Assertions.assertEquals(1.0 / 6.0, agg.finish(), 0.001);
  }

  @Test
  @DisplayName("Numerator all of vocab. Denominator all of vocab.")
  public void test3() {
    final MarkerAggregator<Double> agg = createClassUnderTest(Set.of("1", "2", "3", "4", "5", "6"),
        Set.of("1", "2", "3", "4", "5", "6"),
        Set.of("1", "2", "3", "4", "5", "6"),
        0);
    agg.addValue(new String[] { "6" });
    agg.addValue(new String[] { "5" });
    agg.addValue(new String[] { "4" });
    agg.addValue(new String[] { "3" });
    agg.addValue(new String[] { "2" });
    agg.addValue(new String[] { "1" });
    Assertions.assertEquals(1.0,  agg.finish(), 0.001);
  }

  @Test
  @DisplayName("Numerator most of vocab. Denominator all of vocab.")
  public void test4() {
    final MarkerAggregator<Double> agg = createClassUnderTest(Set.of("1", "3", "4", "5", "6"),
        Set.of("1", "2", "3", "4", "5", "6"),
        Set.of("1", "2", "3", "4", "5", "6"),
        0);
    agg.addValue(new String[] { "6" });
    agg.addValue(new String[] { "5" });
    agg.addValue(new String[] { "4" });
    agg.addValue(new String[] { "3" });
    agg.addValue(new String[] { "2" });
    agg.addValue(new String[] { "1" });
    Assertions.assertEquals(5.0 / 6.0,  agg.finish(), 0.001);
  }

  private MarkerAggregator<Double> createClassUnderTest(Set<String> numeratorValues, Set<String> denominatorValues, Set<String> vocabValues, int index) {
    CategoricalAggregationConfig c = new CategoricalAggregationConfigImpl();
    c.setNumeratorValues(numeratorValues.stream().toList());
    c.setDenominatorValues(denominatorValues.stream().toList());
    return new CategoricalProportionAggregator.CategoricalProportionAggregatorFactory(c, () -> vocabValues.stream().toList())
        .create(index, Double::valueOf);
  }
}
