package org.veupathdb.service.eda.compute.plugins.example;

import jakarta.ws.rs.BadRequestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.compute.plugins.PluginConfigValidator;
import org.veupathdb.service.eda.generated.model.ExampleComputeConfig;
import org.veupathdb.service.eda.generated.model.ExamplePluginRequest;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.util.function.Supplier;

public class ExamplePluginInputValidator implements PluginConfigValidator<ExamplePluginRequest> {

  private static final Logger LOG = LogManager.getLogger(ExamplePluginInputValidator.class);

  @Override
  public void validate(ExamplePluginRequest request, Supplier<ReferenceMetadata> referenceMetadata) {

    ExampleComputeConfig config = request.getConfig();
    VariableSpec inputVar = config.getInputVariable();

    LOG.info("Checking var " + VariableDef.toDotNotation(inputVar));

    // check entity
    EntityDef entity = referenceMetadata.get().getEntity(inputVar.getEntityId())
        .orElseThrow(() -> new BadRequestException("Invalid entity ID : " + inputVar.getEntityId()));

    // check variable
    VariableDef variable = entity.getVariable(inputVar)
        .orElseThrow(() -> new BadRequestException("Invalid variable spec for output entity"));

    // check that suffix is present
    if (config.getValueSuffix() == null || config.getValueSuffix().contains("\t")) {
      throw new BadRequestException("valueSuffix is required and cannot contain tabs");
    }
  }

}
