package org.veupathdb.service.eda.ds.constraints;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.VariableSpec;

public class DataElementValidator {

  private final ReferenceMetadata _referenceMetadata;
  private final ConstraintSpec _constraintSpec;

  public DataElementValidator(ReferenceMetadata referenceMetadata, ConstraintSpec constraintSpec) {
    _referenceMetadata = referenceMetadata;
    _constraintSpec = constraintSpec;
  }

  public void validate(DataElementSet values) throws ValidationException {
  }

  private EntityDef validateEntityAndGet(String entityId) throws ValidationException {
    return _referenceMetadata.getEntity(entityId).orElseThrow(() ->
        new ValidationException("No entity exists on study '" + _referenceMetadata.getStudyId() + "' with ID '" + entityId + "'."));
  }

  private Optional<VariableDef> validateVariableAndGet(VariableSpec variableSpec, ValidationBundle.ValidationBundleBuilder validation) {
    Optional<VariableDef> var = _referenceMetadata.getVariable(variableSpec);
    if (var.isEmpty()) {
      validation.addError("No variable exists on study '" + _referenceMetadata.getStudyId() + "' with spec '" + variableSpec + "'.");
    }
    return var;
  }

  private void validateVariableName(ValidationBundle.ValidationBundleBuilder validation,
                                   EntityDef entity, String variableUse, VariableSpec variable) {
    List<APIVariableType> nonCategoryTypes = Arrays.stream(APIVariableType.values())
        .filter(type -> !type.equals(APIVariableType.CATEGORY))
        .collect(Collectors.toList());
    validateVariableNameAndType(validation, entity, variableUse, variable, nonCategoryTypes.toArray(new APIVariableType[0]));
  }

  private void validateVariableNameAndType(ValidationBundle.ValidationBundleBuilder validation,
                                          EntityDef entity, String variableUse, VariableSpec varSpec, APIVariableType... allowedTypes) {
    List<APIVariableType> allowedTypesList = Arrays.asList(allowedTypes);
    if (allowedTypesList.contains(APIVariableType.CATEGORY)) {
      throw new RuntimeException("Plugin should not be using categories as variables.");
    }
    String varDesc = "Variable " + JsonUtil.serializeObject(varSpec) + ", used for " + variableUse + ", ";
    Optional<VariableDef> var = entity.getVariable(varSpec);
    if (var.isEmpty()) {
      validation.addError(varDesc + "does not exist in entity " + entity.getId());
    }
    else if (!allowedTypesList.contains(var.get().getType())) {
      validation.addError(varDesc + "must be one of the following types: " + FormatUtil.join(allowedTypes, ", "));
    }
  }
}
