package org.veupathdb.service.eda.merge.plugins.transforms;

import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.model.Units.Unit;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.BodyMassIndexConfig;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.merge.core.derivedvars.Transform;

import java.util.List;
import java.util.Map;

public class BodyMassIndex extends Transform<BodyMassIndexConfig> {

  private VariableSpec _heightVar;
  private String _heightColumn;
  private Unit _heightUnit;
  private VariableSpec _weightVar;
  private String _weightColumn;
  private Unit _weightUnit;

  @Override
  protected Class<BodyMassIndexConfig> getConfigClass() {
    return BodyMassIndexConfig.class;
  }

  @Override
  protected void acceptConfig(BodyMassIndexConfig config) throws ValidationException {
    _heightVar = config.getHeightVariable();
    _heightColumn = VariableDef.toDotNotation(_heightVar);
    _weightVar = config.getWeightVariable();
    _weightColumn = VariableDef.toDotNotation(_weightVar);
  }

  @Override
  protected void performSupplementalDependedVariableValidation() throws ValidationException {
    checkVariable("Height", _heightVar, List.of(APIVariableType.INTEGER, APIVariableType.NUMBER), null);
    _heightUnit = findVariableUnit(_heightVar, "height", Unit.METER);
    checkVariable("Weight", _weightVar, List.of(APIVariableType.INTEGER, APIVariableType.NUMBER), null);
    _weightUnit = findVariableUnit(_weightVar, "weight", Unit.KILOGRAM);
  }

  private Unit findVariableUnit(VariableSpec variable, String varDescription, Unit requiredUnitCompatibility) throws ValidationException {
    return _metadata.getVariable(variable).orElseThrow()
        .getUnits()
        .flatMap(Unit::findUnit)
        .filter(unit -> unit.isCompatibleWith(requiredUnitCompatibility))
        .orElseThrow(() -> new ValidationException("Selected " + varDescription + " variable must have a unit compatible with " + requiredUnitCompatibility));
  }

  @Override
  public String getFunctionName() {
    return "bodyMassIndex";
  }

  @Override
  public List<VariableSpec> getRequiredInputVars() {
    return List.of(_heightVar, _weightVar);
  }

  @Override
  public APIVariableType getVariableType() {
    return APIVariableType.NUMBER;
  }

  @Override
  public APIVariableDataShape getDataShape() {
    return APIVariableDataShape.CONTINUOUS;
  }

  @Override
  public String getValue(Map<String, String> row) {
    String rawHeight = row.get(_heightColumn);
    String rawWeight = row.get(_weightColumn);
    // if either value is missing return missing
    if (rawHeight.isBlank() || rawWeight.isBlank()) return EMPTY_VALUE;
    // BMI = kg/(m^2), convert units if needed
    double height = Unit.METER.convertFrom(_heightUnit, Double.parseDouble(rawHeight));
    double weight = Unit.KILOGRAM.convertFrom(_weightUnit, Double.parseDouble(rawWeight));
    return String.valueOf(weight / Math.pow(height, 2));
  }
}
