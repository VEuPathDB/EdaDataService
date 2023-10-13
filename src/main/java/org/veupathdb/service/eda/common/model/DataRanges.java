package org.veupathdb.service.eda.common.model;

import org.gusdb.fgputil.Tuples;
import org.veupathdb.service.eda.generated.model.APICollection;
import org.veupathdb.service.eda.generated.model.APIDateCollection;
import org.veupathdb.service.eda.generated.model.APIDateDistributionDefaults;
import org.veupathdb.service.eda.generated.model.APIDateVariable;
import org.veupathdb.service.eda.generated.model.APIIntegerCollection;
import org.veupathdb.service.eda.generated.model.APIIntegerDistributionDefaults;
import org.veupathdb.service.eda.generated.model.APIIntegerVariable;
import org.veupathdb.service.eda.generated.model.APINumberCollection;
import org.veupathdb.service.eda.generated.model.APINumberDistributionDefaults;
import org.veupathdb.service.eda.generated.model.APINumberVariable;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableWithValues;

import java.util.Optional;

/**
 * Contains a pair of data ranges, one for full data range and one for display range.  This class takes the typed
 * variable (or collection) metadata objects and extracts their type-specific data ranges for generic storage here.
 */
public class DataRanges extends Tuples.TwoTuple<DataRange, DataRange> {

  public DataRanges(DataRange dataRange, DataRange displayRange) {
    super(dataRange, displayRange);
  }

  public DataRanges(DataRange dataRange) {
    super(dataRange, dataRange);
  }

  public DataRange getDataRange() {
    return getFirst();
  }

  public DataRange getDisplayRange() {
    return getSecond();
  }

  public static Optional<DataRanges> getDataRanges(APICollection obj) {
    return obj.getDataShape() != APIVariableDataShape.CONTINUOUS ? Optional.empty() :
      switch (obj.getType()) {
        case NUMBER -> getNumberRanges(((APINumberCollection)obj).getDistributionDefaults());
        case INTEGER -> getIntegerRanges(((APIIntegerCollection)obj).getDistributionDefaults());
        case DATE -> getDateRanges(((APIDateCollection)obj).getDistributionDefaults());
        default -> Optional.empty();
    };
  }

  public static Optional<DataRanges> getDataRanges(APIVariableWithValues obj) {
    return obj.getDataShape() != APIVariableDataShape.CONTINUOUS ? Optional.empty() :
      switch(obj.getType()) {
        case NUMBER -> getNumberRanges(((APINumberVariable)obj).getDistributionDefaults());
        case INTEGER -> getIntegerRanges(((APIIntegerVariable)obj).getDistributionDefaults());
        case DATE -> getDateRanges(((APIDateVariable)obj).getDistributionDefaults());
        default -> Optional.empty();
    };
  }

  private static Optional<DataRanges> getDateRanges(APIDateDistributionDefaults dist) {
    return Optional.of(new DataRanges(
        new DataRange(
            dist.getRangeMin(),
            dist.getRangeMax()),
        new DataRange(
            Optional.ofNullable(dist.getDisplayRangeMin()).orElse(dist.getRangeMin()),
            Optional.ofNullable(dist.getDisplayRangeMax()).orElse(dist.getRangeMax()))));
  }

  private static Optional<DataRanges> getIntegerRanges(APIIntegerDistributionDefaults dist) {
    return Optional.of(new DataRanges(
        new DataRange(
            dist.getRangeMin().toString(),
            dist.getRangeMax().toString()),
        new DataRange(
            Optional.ofNullable(dist.getDisplayRangeMin()).orElse(dist.getRangeMin()).toString(),
            Optional.ofNullable(dist.getDisplayRangeMax()).orElse(dist.getRangeMax()).toString())));
  }

  private static Optional<DataRanges> getNumberRanges(APINumberDistributionDefaults dist) {
    return Optional.of(new DataRanges(
        new DataRange(
            dist.getRangeMin().toString(),
            dist.getRangeMax().toString()),
        new DataRange(
            Optional.ofNullable(dist.getDisplayRangeMin()).orElse(dist.getRangeMin()).toString(),
            Optional.ofNullable(dist.getDisplayRangeMax()).orElse(dist.getRangeMax()).toString())));
  }
}
