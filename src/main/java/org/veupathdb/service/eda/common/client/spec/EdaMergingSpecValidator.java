package org.veupathdb.service.eda.common.client.spec;

import org.gusdb.fgputil.validation.ValidationBundle;
import org.veupathdb.service.eda.common.model.VariableDef;

public class EdaMergingSpecValidator extends StreamSpecValidator {

  @Override
  protected void enforceServiceSpecificRequirements(StreamSpec streamSpec, VariableDef variableDef, ValidationBundle.ValidationBundleBuilder validation) {
    /* nothing to do here */
  }
}
