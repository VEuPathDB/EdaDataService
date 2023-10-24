package org.veupathdb.service.eda.common.client.spec;

import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.common.model.VariableSource;

public class EdaSubsettingSpecValidator extends StreamSpecValidator {

  @Override
  protected void enforceServiceSpecificRequirements(StreamSpec streamSpec, VariableDef variableDef, ValidationBundleBuilder validation) {
    if (!variableDef.getSource().equals(VariableSource.NATIVE)) {
      validation.addError(streamSpec.getStreamName(),
          "Bad spec for subsetting request.  Variable '" + variableDef.getVariableId() +
              "' must be a native variable of entity '" + streamSpec.getEntityId() + "'.");
    }
  }
}
