package org.veupathdb.service.edads.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPList;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.veupathdb.service.edads.generated.model.APIVariableType;
import org.veupathdb.service.edads.generated.model.BoxplotPostRequest;
import org.veupathdb.service.edads.generated.model.BoxplotSpec;
import org.veupathdb.service.edads.util.AbstractEdadsPlugin;
import org.veupathdb.service.edads.util.EntityDef;
import org.veupathdb.service.edads.util.StreamSpec;

public class BoxplotPlugin extends AbstractEdadsPlugin<BoxplotPostRequest, BoxplotSpec> {

  private static final String DATAFILE_NAME = "file1.txt";

  @Override
  protected Class<BoxplotSpec> getAnalysisSpecClass() {
    return BoxplotSpec.class;
  }

  @Override
  protected ValidationBundle validateAnalysisSpec(BoxplotSpec pluginSpec) throws ValidationException {
    ValidationBundleBuilder validation = ValidationBundle.builder(ValidationLevel.RUNNABLE);
    EntityDef entity = getValidEntity(validation, pluginSpec.getEntityId());
    validateVariableNameAndType(validation, entity, "xAxisVariable", pluginSpec.getXAxisVariable(), APIVariableType.STRING);
    validateVariableNameAndType(validation, entity, "yAxisVariable", pluginSpec.getYAxisVariable(), APIVariableType.NUMBER);
    if (pluginSpec.getOverlayVariable() != null) {
      validateVariableNameAndType(validation, entity, "overlayVariable", pluginSpec.getOverlayVariable(), APIVariableType.STRING);
    }
    if (pluginSpec.getFacetVariable() != null) {
      for (String facetVar : pluginSpec.getFacetVariable()) {
        validateVariableNameAndType(validation, entity, "facetVariable", facetVar, APIVariableType.STRING);
      }
    }
    return validation.build();
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(BoxplotSpec pluginSpec) {
    StreamSpec spec = new StreamSpec(DATAFILE_NAME, pluginSpec.getEntityId());
    spec.add(pluginSpec.getXAxisVariable());
    spec.add(pluginSpec.getYAxisVariable());
    if (pluginSpec.getOverlayVariable() != null) {
      spec.add(pluginSpec.getOverlayVariable());
    }
    if (pluginSpec.getFacetVariable() != null) {
      spec.addAll(pluginSpec.getFacetVariable());
    }
    return new ListBuilder<StreamSpec>(spec).toList();
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      BoxplotSpec spec = getPluginSpec();
      connection.voidEval("data <- fread('" + DATAFILE_NAME + "')");
      String overlayVar = ((spec.getOverlayVariable() == null) ? "" : spec.getOverlayVariable());
      String facetVar1 = ((spec.getFacetVariable() == null) ? "" : spec.getFacetVariable().get(0));
      String facetVar2 = ((spec.getFacetVariable() == null) ? "" : spec.getFacetVariable().get(1));
      connection.voidEval("map <- data.frame("
          + "'id'=c('xAxisVariable', "
          + "       'yAxisVariable', "
          + "       'overlayVariable', "
          + "       'facetVariable1', "
          + "       'facetVariable2'), "
          + "'plotRef'=c('" + spec.getXAxisVariable() + "'"
          + ", '" +           spec.getYAxisVariable() + "'"
          + ", '" +           overlayVar + "'"
          + ", '" +           facetVar1 + "'"
          + ", '" +           facetVar2 + "'), stringsAsFactors=FALSE)");
      String outFile = connection.eval("box(data, map, '" + spec.getPoints().toString().toLowerCase() + "', '" + spec.getMean().toString().toLowerCase() + "')").asString();
      RFileInputStream response = connection.openFile(outFile);
      IoUtil.transferStream(out, response);
      response.close();
      out.flush();
    });
  }
}