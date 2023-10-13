package org.veupathdb.service.eda.merge.plugins.reductions;

import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.SingleNumericVarReductionConfig;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.merge.core.derivedvars.Reduction;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class SingleNumericVarReduction extends Reduction<SingleNumericVarReductionConfig> {

  protected abstract class SingleNumericVarReducer implements Reducer {

    protected abstract void processValue(double d);

    @Override
    public void addRow(Map<String, String> nextRow) {
      String s = nextRow.get(_inputColumnName);
      if (s.isBlank()) {
        if (_imputeZero) {
          processValue(0);
        }
        // otherwise, skip empty values (TBD: is this a good API?)
      }
      else {
        processValue(Double.parseDouble(s));
      }
    }
  }

  protected VariableSpec _inputVariable;
  protected String _inputColumnName;
  protected boolean _imputeZero;

  @Override
  protected Class<SingleNumericVarReductionConfig> getConfigClass() {
    return SingleNumericVarReductionConfig.class;
  }

  @Override
  protected void acceptConfig(SingleNumericVarReductionConfig config) {
    _inputVariable = config.getInputVariable();
    _inputColumnName = VariableDef.toDotNotation(_inputVariable);
    _imputeZero = Optional.ofNullable(config.getImputeZero()).orElse(false);
  }

  @Override
  public List<VariableSpec> getRequiredInputVars() {
    return List.of(_inputVariable);
  }

  @Override
  public void performSupplementalDependedVariableValidation() throws ValidationException {
    checkVariable("Input", _inputVariable, List.of(APIVariableType.INTEGER, APIVariableType.NUMBER), null);
  }

  @Override
  public APIVariableType getVariableType() {
    return APIVariableType.NUMBER;
  }

  @Override
  public APIVariableDataShape getDataShape() {
    return APIVariableDataShape.CONTINUOUS;
  }
}
