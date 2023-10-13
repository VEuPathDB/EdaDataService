package org.veupathdb.service.eda.merge.plugins.transforms;

import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.model.Units.Unit;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.UnitConversionConfig;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.merge.core.derivedvars.Transform;

import java.util.List;
import java.util.Map;

public class UnitConversion extends Transform<UnitConversionConfig> {

  private VariableSpec _inputVariable;
  private String _inputColumn;
  private String _userSpecifiedOutputUnit;
  private Unit _inputUnit;
  private Unit _outputUnit;

  @Override
  protected Class<UnitConversionConfig> getConfigClass() {
    return UnitConversionConfig.class;
  }

  @Override
  protected void acceptConfig(UnitConversionConfig config) throws ValidationException {
    _inputVariable = config.getInputVariable();
    _inputColumn = VariableDef.toDotNotation(_inputVariable);
    _userSpecifiedOutputUnit = config.getOutputUnits();
    _outputUnit = Unit.findUnit(_userSpecifiedOutputUnit).orElseThrow(() ->
        new ValidationException("Output unit '" + _userSpecifiedOutputUnit + "' is not a valid unit"));
  }

  @Override
  protected void performSupplementalDependedVariableValidation() throws ValidationException {
    String varUnit = _metadata.getVariable(_inputVariable).orElseThrow().getUnits().orElseThrow(() ->
        new ValidationException("Input variable must have convertible units to convert to a different units."));
    _inputUnit = Unit.findUnit(varUnit).orElseThrow(() ->
        new ValidationException("Variable '" + _inputColumn + "' has a unit '" + varUnit + "' that is not convertible to other units."));
    if (!_inputUnit.isCompatibleWith(_outputUnit))
      throw new ValidationException("Output unit " + _outputUnit + " is not compatible with input variable's unit " + _inputUnit);
  }

  @Override
  public String getFunctionName() {
    return "unitConversion";
  }

  @Override
  public List<VariableSpec> getRequiredInputVars() {
    return List.of(_inputVariable);
  }

  @Override
  public APIVariableType getVariableType() {
    return _metadata.getVariable(_inputVariable).orElseThrow().getType();
  }

  @Override
  public APIVariableDataShape getDataShape() {
    return _metadata.getVariable(_inputVariable).orElseThrow().getDataShape();
  }

  @Override
  public String getUnits() {
    return _userSpecifiedOutputUnit;
  }

  @Override
  public String getValue(Map<String, String> row) {
    String inValue = row.get(_inputColumn);
    return inValue.isEmpty() ? EMPTY_VALUE : String.valueOf(_inputUnit.convertTo(_outputUnit, Double.parseDouble(inValue)));
  }

}
