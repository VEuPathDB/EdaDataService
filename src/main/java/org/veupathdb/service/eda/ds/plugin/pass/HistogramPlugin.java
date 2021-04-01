package org.veupathdb.service.eda.ds.plugin.pass;

import java.util.List;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.HistogramPostRequest;
import org.veupathdb.service.eda.generated.model.HistogramSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;

public abstract class HistogramPlugin<S extends HistogramPostRequest, T extends HistogramSpec> extends AbstractPlugin<S, T> {

  protected static final String DATAFILE_NAME = "file1.txt";

  @Override
  public String getDisplayName() {
    return "Histogram";
  }

  @Override
  public String getDescription() {
    return "Visualize the distribution of a continuous variable";
  }

  @Override
  protected ValidationBundle validateVisualizationSpec(T pluginSpec) throws ValidationException {
    ValidationBundleBuilder validation = ValidationBundle.builder(ValidationLevel.RUNNABLE);
    EntityDef entity = getValidEntity(validation, pluginSpec.getOutputEntityId());
    validateVariableNameAndType(validation, entity, "xAxisVariable", pluginSpec.getXAxisVariable(), APIVariableType.NUMBER, APIVariableType.DATE);
    if (pluginSpec.getOverlayVariable() != null) {
      validateVariableNameAndType(validation, entity, "overlayVariable", pluginSpec.getOverlayVariable(), APIVariableType.STRING);
    }
    if (pluginSpec.getFacetVariable() != null) {
      for (VariableSpec facetVar : pluginSpec.getFacetVariable()) {
        validateVariableNameAndType(validation, entity, "facetVariable", facetVar, APIVariableType.STRING);
      }
    }
    return validation.build();
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(T pluginSpec) {
    StreamSpec spec = new StreamSpec(DATAFILE_NAME, pluginSpec.getOutputEntityId());
    spec.add(pluginSpec.getXAxisVariable());
    if (pluginSpec.getOverlayVariable() != null) {
      spec.add(pluginSpec.getOverlayVariable());
    }    
    if (pluginSpec.getFacetVariable() != null) {
      spec.addAll(pluginSpec.getFacetVariable());
    }
    return new ListBuilder<StreamSpec>(spec).toList();
  }
}