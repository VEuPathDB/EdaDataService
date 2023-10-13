package org.veupathdb.service.eda.merge.plugins.reductions;

import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.generated.model.APIFilter;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.SubsetMembershipConfig;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.merge.core.derivedvars.Reduction;

import java.util.List;
import java.util.Map;

public class SubsetMembership extends Reduction<SubsetMembershipConfig> {

  public static final String RETURNED_TRUE_VALUE = "1";
  public static final String RETURNED_FALSE_VALUE = "0";

  private List<APIFilter> _subsetFilters;

  @Override
  protected Class<SubsetMembershipConfig> getConfigClass() {
    return SubsetMembershipConfig.class;
  }

  @Override
  protected void acceptConfig(SubsetMembershipConfig config) {
    _subsetFilters = config.getSubsetFilters();
  }

  @Override
  protected void performSupplementalDependedVariableValidation() throws ValidationException {
    // nothing to do here
  }

  @Override
  public String getFunctionName() {
    return "subsetMembership";
  }

  @Override
  public List<VariableSpec> getRequiredInputVars() {
    return List.of(_metadata.getEntity(getEntityId()).orElseThrow().getIdColumnDef());
  }

  @Override
  protected List<APIFilter> getFiltersOverride() {
    return _subsetFilters;
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
    return List.of(RETURNED_TRUE_VALUE, RETURNED_FALSE_VALUE);
  }

  @Override
  public Reducer createReducer() {
    return new Reducer() {

      private String _returnValue = RETURNED_FALSE_VALUE;

      @Override
      public void addRow(Map<String, String> nextRow) {
        // row is in subset if any rows returned from child stream
        _returnValue = RETURNED_TRUE_VALUE;
      }

      @Override
      public String getResultingValue() {
        return _returnValue;
      }
    };
  }
}
