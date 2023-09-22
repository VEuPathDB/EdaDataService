package org.veupathdb.service.eda.merging.plugins.transforms;

import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.generated.model.APIFilter;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.DerivedVariableSpec;
import org.veupathdb.service.eda.generated.model.DerivedVariableSpecImpl;
import org.veupathdb.service.eda.generated.model.RelatedObservationMinTimeIntervalConfig;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.merging.core.derivedvars.Transform;
import org.veupathdb.service.eda.merging.plugins.reductions.RelativeObservationAggregator;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.gusdb.fgputil.FormatUtil.TAB;

public class RelativeObservationCalculator extends Transform<RelatedObservationMinTimeIntervalConfig> {

  public static class RelativeObservationAggregatorConfig {

    public final String varDescription;
    public final VariableSpec variable;
    public final VariableSpec timestampVariable;
    public final List<String> trueValues;
    public final List<APIFilter> filtersOverride;

    public RelativeObservationAggregatorConfig(String varDescription, VariableSpec variable,
        VariableSpec timestampVariable, List<String> trueValues, List<APIFilter> filtersOverride) {
      this.varDescription = varDescription;
      this.variable = variable;
      this.timestampVariable = timestampVariable;
      this.trueValues = trueValues;
      this.filtersOverride = filtersOverride;
    }
  }

  public static final String FUNCTION_NAME = "relativeObservationCalculator";

  public static final long MILLIS_PER_DAY = 1000 * 60 * 60 * 24;

  private RelatedObservationMinTimeIntervalConfig _config;
  private int _minTimeIntervalDays;
  private DerivedVariableSpec _anchorSpec;
  private String _anchorDataColumn;
  private DerivedVariableSpec _targetSpec;
  private String _targetDataColumn;

  @Override
  protected Class<RelatedObservationMinTimeIntervalConfig> getConfigClass() {
    return RelatedObservationMinTimeIntervalConfig.class;
  }

  @Override
  protected void acceptConfig(RelatedObservationMinTimeIntervalConfig config) throws ValidationException {
    _config = config;
    Integer minTimeIntervalDays = _config.getMinimumTimeIntervalDays();
    _minTimeIntervalDays = minTimeIntervalDays == null || minTimeIntervalDays < 0 ? 0 : minTimeIntervalDays;
    _anchorSpec = createAggregatorSpec("anchor", _config.getAnchorVariable(),
        _config.getAnchorTimestampVariable(), _config.getAnchorVariableTrueValues());
    _anchorDataColumn = VariableDef.toDotNotation(_anchorSpec);
    _targetSpec = createAggregatorSpec("target", _config.getTargetVariable(),
        _config.getTargetTimestampVariable(), _config.getTargetVariableTrueValues());
    _targetDataColumn = VariableDef.toDotNotation(_targetSpec);
  }

  private DerivedVariableSpec createAggregatorSpec(String varNameSuffix, VariableSpec variable, VariableSpec timestampVariable, List<String> trueValues) {
    DerivedVariableSpec aggregatorSpec = new DerivedVariableSpecImpl();
    aggregatorSpec.setEntityId(getEntityId());
    aggregatorSpec.setVariableId(getVariableId() + "_" + varNameSuffix);
    aggregatorSpec.setFunctionName(RelativeObservationAggregator.FUNCTION_NAME);
    aggregatorSpec.setDisplayName(getVariableId() + "_" + varNameSuffix);
    aggregatorSpec.setConfig(new RelativeObservationAggregatorConfig(
        varNameSuffix, variable, timestampVariable, trueValues, _config.getRelatedObservationsSubset()));
    return aggregatorSpec;
  }

  @Override
  protected void performSupplementalDependedVariableValidation() throws ValidationException {
    // nothing to do here
  }

  @Override
  public String getFunctionName() {
    return FUNCTION_NAME;
  }

  @Override
  public List<DerivedVariableSpec> getDependedDerivedVarSpecs() {
    return List.of(_anchorSpec, _targetSpec);
  }

  @Override
  public List<VariableSpec> getRequiredInputVars() {
    return List.of(_anchorSpec, _targetSpec);
  }

  @Override
  public APIVariableType getVariableType() {
    return APIVariableType.STRING;
  }

  @Override
  public APIVariableDataShape getDataShape() {
    return APIVariableDataShape.CONTINUOUS;
  }

  @Override
  public String getValue(Map<String, String> row) {

    // get the aggregated target values into an easy-to-compare form
    JSONArray targetValueArray = new JSONArray(row.get(_targetDataColumn));
    List<Long> targetEventEpochDays = new ArrayList<>();
    for (int i = 0; i < targetValueArray.length(); i++) {
      // pull timestamp out of each qualifying event and convert to epoch days
      targetEventEpochDays.add(toEpochDays(targetValueArray.getString(i).split(TAB)[1]));
    }
    // sort ascending so the first found is always the earliest
    targetEventEpochDays.sort(Long::compareTo);

    // get the aggregated anchor values
    JSONObject result = new JSONObject();
    JSONArray anchorValueArray = new JSONArray(row.get(_anchorDataColumn));
    for (int i = 0; i < anchorValueArray.length(); i++) {
      // should create 2 tokens: [ id, timestamp ]
      String[] tokens = anchorValueArray.getString(i).split(TAB);
      long anchorDays = toEpochDays(tokens[1]);
      for (Long targetDays : targetEventEpochDays) {
        long dayDiff = targetDays - anchorDays;
        if (dayDiff > _minTimeIntervalDays) {
          // found a match (and by definition the minimum match)
          result.put(tokens[0], dayDiff);
          break;
        }
      }
    }
    return result.toString();
  }

  private static long toEpochDays(String dateString) {
    return FormatUtil.parseDateTime(dateString).toInstant(ZoneOffset.UTC).toEpochMilli() / MILLIS_PER_DAY;
  }
}
