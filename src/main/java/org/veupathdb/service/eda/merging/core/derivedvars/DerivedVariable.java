package org.veupathdb.service.eda.merging.core.derivedvars;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.generated.model.DerivedVariableMetadata;
import org.veupathdb.service.eda.generated.model.DerivedVariableSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.util.List;

/**
 * Provides top-level interface for a DerivedVariable.  The methods provided are those called by the surrounding
 * derived variable framework, which all derived variables must implement (though many defaults are implemented by
 * AbstractDerivedVariable and its children).  An instance of the implementing class is created for each specified
 * configuration in the request.  In that sense, and instance of this interface is a "plugin" but also an instance
 * of the derived variable itself.  See methods in DerivedVariableFactory for how instances are created.
 */
// only serialize metadata fields since these are used directly to output metadata (not converted to DerivedVariableMetadataImpl)
@JsonSerialize(as=DerivedVariableMetadata.class)
@JsonPropertyOrder({
    "entityId",
    "variableId",
    "derivationType",
    "variableType",
    "dataShape",
    "vocabulary",
    "units",
    "dataRange"
})
public interface DerivedVariable extends DerivedVariableMetadata {

  /**
   * Universal empty value; indicates 'null' or missing data in a tabular response and should be used in lieu of null.
   */
  String EMPTY_VALUE = "";

  /**
   * @return name of this derived variable plugin; this is the
   * name used by clients to identify the desired plugin
   */
  String getFunctionName();

  /**
   * Initializes this derived variable instance and assigns and validates config
   *
   * @param metadata metadata for the appropriate study
   * @param spec configuration for this derived variable instance
   */
  void init(ReferenceMetadata metadata, DerivedVariableSpec spec) throws ValidationException;

  /**
   * @return entity of this derived variable
   */
  EntityDef getEntity();

  /**
   * @return column name for this derived variable (dot notation)
   */
  String getColumnName();

  /**
   * @return all required input vars for this derived variable
   */
  List<VariableSpec> getRequiredInputVars();

  /**
   * Validate that the variable specs this derived variable
   * depends on exist and are available on a permitted entity
   */
  void validateDependedVariables() throws ValidationException;

  /**
   * @return any additional derived variables that need to be
   * generated to support this derived var
   */
  List<DerivedVariableSpec> getDependedDerivedVarSpecs();

  /**
   * @return display name for this derived variable
   */
  String getDisplayName();

}
