package org.veupathdb.service.eda.merging.plugins.transforms;

import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.CategoricalRecodingConfig;
import org.veupathdb.service.eda.generated.model.CategoricalRecodingRule;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.merging.core.derivedvars.Transform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CategoricalRecoding extends Transform<CategoricalRecodingConfig> {

  private VariableSpec _inputVar;
  private String _inputColumn;
  private List<CategoricalRecodingRule> _recodingRules;
  private String _unmappedValue;

  @Override
  protected Class<CategoricalRecodingConfig> getConfigClass() {
    return CategoricalRecodingConfig.class;
  }

  @Override
  protected void acceptConfig(CategoricalRecodingConfig config) throws ValidationException {
    _inputVar = config.getInputVariable();
    _inputColumn = VariableDef.toDotNotation(_inputVar);
    _recodingRules = config.getRules();
    _unmappedValue = Optional.ofNullable(config.getUnmappedValue()).orElse(EMPTY_VALUE);
  }

  @Override
  protected void performSupplementalDependedVariableValidation() throws ValidationException {
    checkVariable("Input", _inputVar, null, List.of(APIVariableDataShape.CATEGORICAL, APIVariableDataShape.BINARY));
  }

  @Override
  public String getFunctionName() {
    return "categoricalRecoding";
  }

  @Override
  public List<VariableSpec> getRequiredInputVars() {
    return List.of(_inputVar);
  }

  @Override
  public APIVariableType getVariableType() {
    return APIVariableType.STRING;
  }

  @Override
  public APIVariableDataShape getDataShape() {
    return APIVariableDataShape.CATEGORICAL;
  }

  @Override
  public List<String> getVocabulary() {
    List<String> vocab = new ArrayList<>(_recodingRules.stream().map(CategoricalRecodingRule::getOutputValue).toList());
    vocab.add(_unmappedValue);
    return vocab;
  }

  @Override
  public String getValue(Map<String, String> row) {
    String inputValue = row.get(_inputColumn);
    for (CategoricalRecodingRule rule : _recodingRules) {
      if (rule.getInputValues().contains(inputValue)) {
        // return output value for the first match
        return rule.getOutputValue();
      }
    }
    return _unmappedValue;
  }
}
