package org.veupathdb.service.eda.merge.plugins.reductions;

import org.gusdb.fgputil.validation.ValidationException;
import org.json.JSONArray;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.generated.model.APIFilter;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.merge.core.derivedvars.Reduction;
import org.veupathdb.service.eda.merge.plugins.transforms.RelativeObservationCalculator.RelativeObservationAggregatorConfig;

import java.util.List;
import java.util.Map;

import static org.gusdb.fgputil.FormatUtil.TAB;

public class RelativeObservationAggregator extends Reduction<RelativeObservationAggregatorConfig> {

  public static final String FUNCTION_NAME = "relativeObservationAggregator";

  private String _varDescription;
  private List<String> _trueValues;
  private VariableSpec _variable;
  private String _variableCol;
  private VariableSpec _timestampVariable;
  private String _timestampCol;
  private String _idCol;
  private List<APIFilter> _filtersOverride;

  @Override
  protected Class<RelativeObservationAggregatorConfig> getConfigClass() {
    return RelativeObservationAggregatorConfig.class;
  }

  @Override
  protected void acceptConfig(RelativeObservationAggregatorConfig config) throws ValidationException {
    _varDescription = config.varDescription;
    _trueValues = config.trueValues;
    _variable = config.variable;
    _variableCol = VariableDef.toDotNotation(_variable);
    _timestampVariable = config.timestampVariable;
    _timestampCol = VariableDef.toDotNotation(_timestampVariable);
    _filtersOverride = config.filtersOverride;
    if (!_variable.getEntityId().equals(_timestampVariable.getEntityId())) {
      throw new ValidationException(_varDescription + " variable must have the same entity as " + _varDescription + " timestamp variable");
    }
  }

  @Override
  protected void performSupplementalDependedVariableValidation() throws ValidationException {
    // need to use the ID of the source entity
    _idCol = VariableDef.toDotNotation(_metadata.getEntity(_variable.getEntityId()).orElseThrow().getIdColumnDef());
    checkVariable(_varDescription + " timestamp", _timestampVariable, List.of(APIVariableType.DATE), null);
  }

  @Override
  public String getFunctionName() {
    return FUNCTION_NAME;
  }

  @Override
  public List<VariableSpec> getRequiredInputVars() {
    return List.of(_variable, _timestampVariable);
  }

  @Override
  protected List<APIFilter> getFiltersOverride() {
    return _filtersOverride;
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
  public Reducer createReducer() {
    return new Reducer() {

      final JSONArray rows = new JSONArray();

      @Override
      public void addRow(Map<String, String> nextRow) {
        String value = nextRow.get(_variableCol);
        if (_trueValues.contains(value)) {
          rows.put(nextRow.get(_idCol) + TAB + nextRow.get(_timestampCol));
        }
      }

      @Override
      public String getResultingValue() {
        return rows.toString();
      }
    };
  }
}
