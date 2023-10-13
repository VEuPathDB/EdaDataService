package org.veupathdb.service.eda.common.client.spec;

import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class StreamSpecValidator {

  protected abstract void enforceServiceSpecificRequirements(StreamSpec streamSpec, VariableDef variableDef, ValidationBundleBuilder validation);

  public ValidationBundle validateStreamSpecs(Collection<StreamSpec> streamSpecs, ReferenceMetadata metadata) {
    ValidationBundleBuilder validation = ValidationBundle.builder(ValidationLevel.RUNNABLE);
    checkUniqueNames(streamSpecs, validation);
    for (StreamSpec streamSpec : streamSpecs) {

      // validate requested entity ID
      Optional<EntityDef> entityOpt = metadata.getEntity(streamSpec.getEntityId());
      if (entityOpt.isEmpty()) {
        validation.addError(streamSpec.getStreamName(), "Entity '" +
            streamSpec.getEntityId() + "' does not exist in study '" + metadata.getStudyId());
        continue;
      }
      EntityDef requestedEntity = entityOpt.get();

      for (VariableSpec varSpec : streamSpec) {
        Optional<VariableDef> varOpt = requestedEntity.getVariable(varSpec);
        if (varOpt.isPresent()) {
          enforceServiceSpecificRequirements(streamSpec, varOpt.get(), validation);
        }
        else {
          validation.addError(streamSpec.getStreamName(),
              "Variable '" + JsonUtil.serializeObject(varSpec) +
              "' must be available on entity '" + streamSpec.getEntityId() + "'.");
        }
      }
    }
    return validation.build();
  }

  protected static void checkUniqueNames(Collection<StreamSpec> streamSpecs, ValidationBundleBuilder validation) {
    Set<String> specNames = streamSpecs.stream().map(StreamSpec::getStreamName).collect(Collectors.toSet());
    if (specNames.size() != streamSpecs.size()) {
      validation.addError("Stream specs must not duplicate names.");
    }
  }
}
