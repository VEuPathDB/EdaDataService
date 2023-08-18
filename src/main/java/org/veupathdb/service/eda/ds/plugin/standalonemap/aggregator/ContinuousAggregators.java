package org.veupathdb.service.eda.ds.plugin.standalonemap.aggregator;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Aggregators that collect variable values and produce some aggregated result.
 */
public enum ContinuousAggregators {
  Mean(org.veupathdb.service.eda.generated.model.Aggregator.MEAN.getValue(),
      new ContinuousAggregatorFactory() {
        @Override
        public MeanAggregator create(int index, Function<String, Double> valueQuantifier) {
          return new MeanAggregator(index, valueQuantifier);
        }

        @Override
        public MarkerAggregator<AveragesWithConfidence> createWithConfidence(int index, Function<String, Double> valueQuantifier) {
          return new MeanWithConfidenceAggregator(index, valueQuantifier);
        }
      }),
  Median(org.veupathdb.service.eda.generated.model.Aggregator.MEDIAN.getValue(),
      new ContinuousAggregatorFactory() {
        @Override
        public MedianAggregator create(int index, Function<String, Double> valueQuantifier) {
          return new MedianAggregator(index, valueQuantifier);
        }

        @Override
        public MarkerAggregator<AveragesWithConfidence> createWithConfidence(int index, Function<String, Double> valueQuantifier) {
          return null;
        }
      });

  private final ContinuousAggregatorFactory factory;
  private final String name;

  ContinuousAggregators(String name,
                        ContinuousAggregatorFactory factory) {
    this.name = name;
    this.factory = factory;
  }

  public ContinuousAggregatorFactory getAggregatorFactory() {
    return factory;
  }

  public MarkerAggregator<AveragesWithConfidence> getAverageWithConfidenceAggregator(int index, Function<String, Double> variableValueQuantifier) {
    return factory.createWithConfidence(index, variableValueQuantifier);
  }

  public static ContinuousAggregators fromExternalString(String name) {
    return Stream.of(values())
        .filter(f -> f.name.equals(name))
        .findFirst()
        .orElseThrow();
  }

  public interface ContinuousAggregatorFactory {
    MarkerAggregator<Double> create(int index, Function<String, Double> valueQuantifier);
    MarkerAggregator<AveragesWithConfidence> createWithConfidence(int index, Function<String, Double> valueQuantifier);
  }
}
