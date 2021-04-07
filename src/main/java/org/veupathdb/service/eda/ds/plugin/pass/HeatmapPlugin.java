package org.veupathdb.service.eda.ds.plugin.pass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.HeatmapPostRequest;
import org.veupathdb.service.eda.generated.model.HeatmapSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class HeatmapPlugin extends AbstractPlugin<HeatmapPostRequest, HeatmapSpec> {

  private static final String DATAFILE_NAME = "file1.txt";

  @Override
  public String getDisplayName() {
    return "Heatmap";
  }

  @Override
  public String getDescription() {
    return "Visualize the magnitude of a continuous numeric variable";
  }

  @Override
  protected Class<HeatmapSpec> getVisualizationSpecClass() {
    return HeatmapSpec.class;
  }

  @Override
  protected void validateVisualizationSpec(HeatmapSpec pluginSpec) throws ValidationException {
    ReferenceMetadata md = getReferenceMetadata();
    ValidationBundleBuilder validation = ValidationBundle.builder(ValidationLevel.RUNNABLE);
    EntityDef entity = md.validateEntityAndGet(pluginSpec.getOutputEntityId());
    md.validateVariableNameAndType(validation, entity, "xAxisVariable", pluginSpec.getXAxisVariable(), APIVariableType.STRING);
    md.validateVariableNameAndType(validation, entity, "yAxisVariable", pluginSpec.getYAxisVariable(), APIVariableType.STRING);
    if (pluginSpec.getZAxisVariable() != null) {
      md.validateVariableNameAndType(validation, entity, "zAxisVariable", pluginSpec.getYAxisVariable(), APIVariableType.NUMBER);
    }
    if (pluginSpec.getFacetVariable() != null) {
      for (VariableSpec facetVar : pluginSpec.getFacetVariable()) {
        md.validateVariableNameAndType(validation, entity, "facetVariable", facetVar, APIVariableType.STRING);
      }
    }
    if (pluginSpec.getValueSpec().equals(HeatmapSpec.ValueSpecType.SERIES) && pluginSpec.getZAxisVariable() == null) {
    	validation.addError("zAxisVariable required for heatmap of type 'series'.");
    }
    validation.build().throwIfInvalid();
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(HeatmapSpec pluginSpec) {
    StreamSpec spec = new StreamSpec(DATAFILE_NAME, pluginSpec.getOutputEntityId());
    spec.add(pluginSpec.getXAxisVariable());
    spec.add(pluginSpec.getYAxisVariable());
    if (pluginSpec.getZAxisVariable() != null) {
      spec.add(pluginSpec.getZAxisVariable());
    }
    if (pluginSpec.getFacetVariable() != null) {
      spec.addAll(pluginSpec.getFacetVariable());
    }
    return new ListBuilder<StreamSpec>(spec).toList();
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      HeatmapSpec spec = getPluginSpec();
      connection.voidEval("data <- fread('" + DATAFILE_NAME + "')");
      connection.voidEval("map <- data.frame("
          + "'plotRef'=c('xAxisVariable', "
          + "       'yAxisVariable', "
          + "       'zAxisVariable', "
          + "       'facetVariable1', "
          + "       'facetVariable2'), "
          + "'id'=c('" + toColNameOrEmpty(spec.getXAxisVariable()) + "'"
          + ", '" + toColNameOrEmpty(spec.getYAxisVariable()) + "'"
          + ", '" + toColNameOrEmpty(spec.getZAxisVariable()) + "'"
          + ", '" + toColNameOrEmpty(spec.getFacetVariable().get(0)) + "'"
          + ", '" + toColNameOrEmpty(spec.getFacetVariable().get(1)) + "'), stringsAsFactors=FALSE)");
      String outFile = connection.eval("heatmap(data, map, '" + spec.getValueSpec().toString().toLowerCase() + "')").asString();
      try (RFileInputStream response = connection.openFile(outFile)) {
        IoUtil.transferStream(out, response);
      }
      out.flush();
    });
  }
}