package org.veupathdb.service.eda.ds.plugin.standalonemap.markers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Produces a collection of aggregations for variable values.
 */
public class CollectionAveragesWithConfidenceAggregator implements MarkerAggregator<Map<String, AveragesWithConfidence>> {
  private final Function<Integer, String> variableIdByIndex;
  private final Map<String, AveragesWithConfidenceAggregator> averageAggregators = new HashMap<>();

  /**
   *
   * @param variableIdByIndex Mapping function to find a variable ID in dot notation based on tabular index.
   * @param indexByVarId Mapping function to find index from a variable ID in dot notation.
   * @param variableIds Collection variables in dot-notation to aggregate the values of.
   * @param variableValueQuantifier Function for mapping string-encoded tabular output to a numerical value to be incorporated into averages.
   */
  public CollectionAveragesWithConfidenceAggregator(Function<Integer, String> variableIdByIndex,
                                                    Function<String, Integer> indexByVarId,
                                                    List<String> variableIds,
                                                    Function<String, Double> variableValueQuantifier) {
    this.variableIdByIndex = variableIdByIndex;
    // Create new aggregators for each collection variable.
    variableIds.forEach(var -> averageAggregators.put(var, new AveragesWithConfidenceAggregator(indexByVarId.apply(var), variableValueQuantifier)));
  }

  @Override
  public void addValue(String[] d) {
    for (int i = 0; i < d.length; i++) {
      String var = variableIdByIndex.apply(i);
      AveragesWithConfidenceAggregator agg = averageAggregators.get(var);
      if (agg != null) {
        agg.addValue(d);
      }
    }
  }

  @Override
  public Map<String, AveragesWithConfidence> finish() {
    return averageAggregators.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().finish()));
  }
}
