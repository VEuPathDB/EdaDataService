package org.veupathdb.service.eda.ds.constraints;

import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;

public class DataElementValidator {

  private final ReferenceMetadata _referenceMetadata;
  private final ConstraintSpec _constraintSpec;

  public DataElementValidator(ReferenceMetadata referenceMetadata, ConstraintSpec constraintSpec) {
    _referenceMetadata = referenceMetadata;
    _constraintSpec = constraintSpec;
  }

  public void validate(DataElementSet values) throws ValidationException {
  }
}
