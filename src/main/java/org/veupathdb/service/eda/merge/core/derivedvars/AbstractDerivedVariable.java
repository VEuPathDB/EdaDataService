package org.veupathdb.service.eda.merge.core.derivedvars;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.BadRequestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.DerivationType;
import org.veupathdb.service.eda.generated.model.DerivedVariableSpec;
import org.veupathdb.service.eda.generated.model.Range;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.generated.model.VariableSpecImpl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides basic functionality all derived variable plugins need, e.g. converting the configuration object into
 * the specified generic param
 *
 * @param <T> type of configuration object for this derived variable plugin
 */
public abstract class AbstractDerivedVariable<T> extends VariableSpecImpl implements DerivedVariable {

  private static final Logger LOG = LogManager.getLogger(AbstractDerivedVariable.class);

  protected ReferenceMetadata _metadata;
  protected String _displayName;

  // cache the column name so it is not calculated for each row
  private String _columnName;

  protected abstract Class<T> getConfigClass();

  /**
   * Read and validate config, assigning local fields as needed to execute derived
   * variable logic later during data processing.  Note this method should NOT
   * try to convert any passed VariableSpecs to VariableDefs.  This will be checked
   * later by the depended variable validation methods.
   *
   * @param config config object sent as part of the request
   * @throws ValidationException if validation fails
   */
  protected abstract void acceptConfig(T config) throws ValidationException;

  // validation method to be implemented by the derived variable type (reduction vs transform)
  protected abstract void validateDependedVariableLocations() throws ValidationException;

  // validation method to be implemented by the specific plugin
  protected abstract void performSupplementalDependedVariableValidation() throws ValidationException;

  public final void validateDependedVariables() throws ValidationException {
    validateDependedVariableLocations();
    performSupplementalDependedVariableValidation();
  }

  protected void checkVariable(String inputName, VariableSpec varSpec, List<APIVariableType> allowedTypesOrNull, List<APIVariableDataShape> allowedShapesOrNull) throws ValidationException {
    VariableDef var = _metadata.getVariable(varSpec).orElseThrow();
    if (allowedTypesOrNull != null && !allowedTypesOrNull.contains(var.getType())) {
      throw new ValidationException(inputName + " variable must be of type: [" + allowedTypesOrNull.stream()
          .map(APIVariableType::getValue).collect(Collectors.joining(", ")) + "]");
    }
    if (allowedShapesOrNull != null && !allowedShapesOrNull.contains(var.getDataShape())) {
      throw new ValidationException(inputName + " variable must be of shape: [" + allowedShapesOrNull.stream()
          .map(APIVariableDataShape::getValue).collect(Collectors.joining(", ")) + "]");
    }
  }

  @Override
  public void init(ReferenceMetadata metadata, DerivedVariableSpec spec) throws ValidationException {
    _metadata = metadata;
    setEntityId(spec.getEntityId());
    setVariableId(spec.getVariableId());
    _displayName = spec.getDisplayName();
    acceptConfig(convertConfig(spec.getConfig()));
  }

  private T convertConfig(Object configObject) {
    if (configObject instanceof Map) {
      try {
        Map<?,?> map = (Map<?,?>)configObject;
        JSONObject jsonObj = new JSONObject(map);
        LOG.info("Received the following config, to be converted to " + getConfigClass().getName() + ":" + FormatUtil.NL + jsonObj.toString(2));
        return JsonUtil.Jackson.readValue(jsonObj.toString(), getConfigClass());
      }
      catch (JSONException | JsonProcessingException e) {
        throw new BadRequestException("Could not coerce config object for spec " +
            "of derived variable " + getFunctionName() + " to type " + getConfigClass().getName());
      }
    }
    throw new BadRequestException("config property for spec of derived variable " +
        getFunctionName() + " must be an object");
  }

  @Override
  public EntityDef getEntity() {
    return _metadata.getEntity(getEntityId()).orElseThrow(
        () -> new BadRequestException("Could not find derived variable entity: " + getEntityId()));
  }

  @Override
  public String getColumnName() {
    // only calculate once
    if (_columnName == null) {
      _columnName = VariableDef.toDotNotation(this);
    }
    return _columnName;
  }

  @Override
  public List<DerivedVariableSpec> getDependedDerivedVarSpecs() {
    return Collections.emptyList();
  }

  @Override
  public String getDisplayName() {
    return _displayName;
  }

  @Override
  public String toString() {
    return "{ functionName: " + getFunctionName() + ", variable: " + VariableDef.toDotNotation(this) + " }";
  }

  // setters of metadata properties are no-ops; used in JSON formatting only
  @Override public void setDerivationType(DerivationType derivationType) { /* noop */ }
  @Override public void setVariableType(APIVariableType variableType) { /* noop */ }
  @Override public void setDataShape(APIVariableDataShape dataShape) { /* noop */ }
  @Override public void setVocabulary(List<String> vocabulary) { /* noop */ }
  @Override public void setUnits(String units) { /* noop */ }
  @Override public void setDataRange(Range dataRange) { /* noop */ }

  // some getters of metadata have default values

  @Override
  public List<String> getVocabulary() {
    return null;
  }

  @Override
  public String getUnits() {
    return null;
  }

  @Override
  public Range getDataRange() {
    return null;
  }
}
