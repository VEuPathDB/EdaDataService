package org.veupathdb.service.edads.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.veupathdb.service.edads.generated.model.APIVariableType;
import org.veupathdb.service.edads.generated.model.ScatterplotPostRequest;
import org.veupathdb.service.edads.generated.model.ScatterplotSpec;
import org.veupathdb.service.edads.util.AbstractEdadsPlugin;
import org.veupathdb.service.edads.util.EntityDef;
import org.veupathdb.service.edads.util.StreamSpec;

public class ScatterplotPlugin extends AbstractEdadsPlugin<ScatterplotPostRequest, ScatterplotSpec> {

  @Override
  protected Class<ScatterplotSpec> getConfigurationClass() {
    return ScatterplotSpec.class;
  }

  @Override
  protected ValidationBundle validateConfig(ScatterplotSpec pluginSpec) throws ValidationException {
    ValidationBundleBuilder validation = ValidationBundle.builder(ValidationLevel.RUNNABLE);
    EntityDef entity = getValidEntity(validation, pluginSpec.getEntityId());
    validateVariableNameAndType(validation, entity, "xAxisVariable", pluginSpec.getXAxisVariable(), APIVariableType.NUMBER, APIVariableType.DATE);
    validateVariableNameAndType(validation, entity, "yAxisVariable", pluginSpec.getYAxisVariable(), APIVariableType.NUMBER, APIVariableType.DATE);
    if (!entity.get(pluginSpec.getXAxisVariable()).getType().equals(entity.get(pluginSpec.getYAxisVariable()).getType())) {
      validation.addError("xAxis and yAxis variables must be the same type.");
    }
    validateVariableNameAndType(validation, entity, "overlayVariable", pluginSpec.getOverlayVariable(), APIVariableType.NUMBER);
    for (String facetVar : pluginSpec.getFacetVariable()) {
      // don't care what type for facets
      validateVariableName(validation, entity, "facetVariable", facetVar);
    }
    return validation.build();
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(ScatterplotSpec pluginSpec) {
    StreamSpec spec = new StreamSpec(pluginSpec.getEntityId());
    spec.add(pluginSpec.getXAxisVariable());
    spec.add(pluginSpec.getYAxisVariable());
    spec.add(pluginSpec.getOverlayVariable());
    spec.addAll(pluginSpec.getFacetVariable());
    return new ListBuilder<StreamSpec>(spec).toList();
  }

  @Override
  protected void writeResults(OutputStream out, List<InputStream> dataStreams) throws IOException {

  }
}
