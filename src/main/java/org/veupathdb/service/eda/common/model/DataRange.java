package org.veupathdb.service.eda.common.model;

import org.gusdb.fgputil.Tuples;
import org.veupathdb.service.eda.generated.model.Range;

import java.util.Optional;
import java.util.function.Supplier;

import static org.gusdb.fgputil.functional.Functions.doThrow;

public class DataRange extends Tuples.TwoTuple<String, String> {

  public DataRange(String min, String max) {
    super(min, max);
  }

  public String getMin() {
    return getFirst();
  }

  public String getMax() {
    return getSecond();
  }

  public static Optional<DataRange> fromRange(Range range) {
    return range == null ? Optional.empty() : fromBoundaryObjects(range.getMin(), range.getMax());
  }

  public static Optional<DataRange> fromBoundaryObjects(Object min, Object max) {
    Supplier<RuntimeException> ex = () -> new RuntimeException(
        "Computed variable display range must contain both min and max or neither.");
    // return empty if both null, range if neither null, and throw ex otherwise
    return min == null
        ? max == null
            ? Optional.empty()
            : doThrow(ex)
        : max == null
            ? doThrow(ex)
            : Optional.of(new DataRange(min.toString(), max.toString()));
  }
}
