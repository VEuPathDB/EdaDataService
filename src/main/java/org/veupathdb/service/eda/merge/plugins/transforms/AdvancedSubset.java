package org.veupathdb.service.eda.merge.plugins.transforms;

import jakarta.ws.rs.BadRequestException;
import org.gusdb.fgputil.functional.Functions;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.AdvancedSubsetConfig;
import org.veupathdb.service.eda.generated.model.SetOperation;
import org.veupathdb.service.eda.generated.model.Step;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.merge.core.derivedvars.Transform;
import org.veupathdb.service.eda.merge.plugins.reductions.SubsetMembership;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class AdvancedSubset extends Transform<AdvancedSubsetConfig> {

  private static final List<String> DEFAULT_TRUE_VALUES = List.of("1", "true", "yes");

  private enum Operation implements BiPredicate<Boolean, Boolean> {
    INTERSECT ((a, b) -> a && b),
    UNION     ((a, b) -> a || b),
    MINUS     ((a, b) -> a && !b);

    private final BiPredicate<Boolean, Boolean> _function;

    Operation(BiPredicate<Boolean, Boolean> function) {
      _function = function;
    }

    public static Operation getByOperationType(SetOperation type) {
      if (type == null) throw new BadRequestException("operation is required");
      return switch (type) {
        case INTERSECT -> INTERSECT;
        case UNION -> UNION;
        case MINUS -> MINUS;
      };
    }

    @Override
    public boolean test(Boolean a, Boolean b) {
      return _function.test(a, b);
    }
  }

  private List<VariableSpec> _requiredVars;
  private OperationNode _operationTree;

  @Override
  protected Class<AdvancedSubsetConfig> getConfigClass() {
    return AdvancedSubsetConfig.class;
  }

  @Override
  protected void acceptConfig(AdvancedSubsetConfig config) throws ValidationException {
    Map<String,Step> stepMap = Functions.getMapFromValues(config.getSteps(), Step::getKey);
    if (stepMap.size() != config.getSteps().size())
      throw new ValidationException("Steps must have unique keys within this request.");
    Map<String, VariableSpec> requiredVarMap = new HashMap<>(); // will be collected during tree creation
    _operationTree = new OperationNode(config.getRootStepKey(), stepMap, requiredVarMap);
    if (!stepMap.isEmpty()) {
      throw new ValidationException("All submitted steps must be used.");
    }
    _requiredVars = new ArrayList<>(requiredVarMap.values());
  }

  @Override
  protected void performSupplementalDependedVariableValidation() {
    // no additional validation needed
  }

  @Override
  public String getFunctionName() {
    return "advancedSubset";
  }

  @Override
  public List<VariableSpec> getRequiredInputVars() {
    return _requiredVars;
  }

  @Override
  public APIVariableType getVariableType() {
    return APIVariableType.INTEGER;
  }

  @Override
  public APIVariableDataShape getDataShape() {
    return APIVariableDataShape.BINARY;
  }

  @Override
  public List<String> getVocabulary() {
    return List.of(
        SubsetMembership.RETURNED_TRUE_VALUE,
        SubsetMembership.RETURNED_FALSE_VALUE);
  }

  @Override
  public String getValue(Map<String, String> row) {
    return _operationTree.test(row)
        ? SubsetMembership.RETURNED_TRUE_VALUE
        : SubsetMembership.RETURNED_FALSE_VALUE;
  }

  private static class OperationNode implements Predicate<Map<String,String>> {

    private final Operation _op;
    private final Predicate<Map<String,String>> _leftChild;
    private final Predicate<Map<String,String>> _rightChild;

    public OperationNode(String stepKey, Map<String, Step> stepMap, Map<String, VariableSpec> requiredVarMap) throws ValidationException {
      Step step = stepMap.remove(stepKey);
      if (step == null)
        throw new ValidationException("Step key '" + stepKey + "' does not correspond to any step's key or is referenced more than once.");
      _op = Operation.getByOperationType(step.getOperation());
      _leftChild = createChild(
          "left",
          step.getLeftStepKey(),
          step.getLeftVariable(),
          step.getLeftVariableTrueValues(),
          stepMap, requiredVarMap);
      _rightChild = createChild(
          "right",
          step.getRightStepKey(),
          step.getRightVariable(),
          step.getRightVariableTrueValues(),
          stepMap, requiredVarMap);
    }

    private Predicate<Map<String,String>> createChild(String childSide, String childKey, VariableSpec childVariable,
        List<String> childTrueValues, Map<String, Step> stepMap, Map<String, VariableSpec> requiredVarMap) throws ValidationException {
      // ensure exactly one of [ stepKey, variable ] is populated
      if ((childKey == null && childVariable == null) ||
          (childKey != null && childVariable != null)) {
        throw new ValidationException("Each step must contain exactly one of: a " + childSide + " step key or " + childSide + " variable spec.");
      }
      if (childVariable != null) {
        String columnName = VariableDef.toDotNotation(childVariable);
        // add var as a required var; map ensures no repeats
        requiredVarMap.put(columnName, childVariable);
        List<String> trueValues = Optional.ofNullable(childTrueValues).orElse(DEFAULT_TRUE_VALUES);
        return row -> trueValues.contains(row.get(columnName));
      }
      else {
        return new OperationNode(childKey, stepMap, requiredVarMap);
      }
    }

    @Override
    public boolean test(Map<String, String> row) {
      return _op.test(_leftChild.test(row), _rightChild.test(row));
    }
  }
}
