package org.veupathdb.service.edads.plugin;

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
import org.veupathdb.service.edads.generated.model.APIVariableType;
import org.veupathdb.service.edads.generated.model.HeatmapPostRequest;
import org.veupathdb.service.edads.generated.model.HeatmapSpec;
import org.veupathdb.service.edads.generated.model.VariableSpec;
import org.veupathdb.service.edads.util.AbstractEdadsPlugin;
import org.veupathdb.service.edads.util.EntityDef;
import org.veupathdb.service.edads.util.StreamSpec;

public class HeatmapPlugin extends AbstractEdadsPlugin<HeatmapPostRequest, HeatmapSpec> {

  private static final String DATAFILE_NAME = "file1.txt";

  @Override
  protected Class<HeatmapSpec> getAnalysisSpecClass() {
    return HeatmapSpec.class;
  }

  @Override
  protected ValidationBundle validateAnalysisSpec(HeatmapSpec pluginSpec) throws ValidationException {
    ValidationBundleBuilder validation = ValidationBundle.builder(ValidationLevel.RUNNABLE);
    EntityDef entity = getValidEntity(validation, pluginSpec.getEntityId());
    validateVariableNameAndType(validation, entity, "xAxisVariable", pluginSpec.getXAxisVariable(), APIVariableType.STRING);
    validateVariableNameAndType(validation, entity, "yAxisVariable", pluginSpec.getYAxisVariable(), APIVariableType.STRING);
    if (pluginSpec.getZAxisVariable() != null) {
      validateVariableNameAndType(validation, entity, "zAxisVariable", pluginSpec.getYAxisVariable(), APIVariableType.NUMBER);
    }
    if (pluginSpec.getFacetVariable() != null) {
      for (VariableSpec facetVar : pluginSpec.getFacetVariable()) {
        validateVariableNameAndType(validation, entity, "facetVariable", facetVar, APIVariableType.STRING);
      }
    }
    if (pluginSpec.getValueSpec().equals(HeatmapSpec.ValueSpecType.SERIES) && pluginSpec.getZAxisVariable() == null) {
    	validation.addError("zAxisVariable required for heatmap of type 'series'.");
    }
    return validation.build();
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(HeatmapSpec pluginSpec) {
    StreamSpec spec = new StreamSpec(DATAFILE_NAME, pluginSpec.getEntityId());
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
      RFileInputStream response = connection.openFile(outFile);
      IoUtil.transferStream(out, response);
      response.close();
      out.flush();
    });
  }
}