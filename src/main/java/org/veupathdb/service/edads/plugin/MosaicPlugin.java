package org.veupathdb.service.edads.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.veupathdb.service.edads.generated.model.MosaicPostRequest;
import org.veupathdb.service.edads.generated.model.MosaicSpec;
import org.veupathdb.service.edads.util.AbstractEdadsPlugin;
import org.veupathdb.service.edads.util.EntityDef;
import org.veupathdb.service.edads.util.StreamSpec;

public class MosaicPlugin extends AbstractEdadsPlugin<MosaicPostRequest, MosaicSpec> {

  private static final String DATAFILE_NAME = "file1.txt";

  @Override
  protected Class<MosaicSpec> getAnalysisSpecClass() {
    return MosaicSpec.class;
  }

  @Override
  protected ValidationBundle validateAnalysisSpec(MosaicSpec pluginSpec) throws ValidationException {
    ValidationBundleBuilder validation = ValidationBundle.builder(ValidationLevel.RUNNABLE);
    EntityDef entity = getValidEntity(validation, pluginSpec.getEntityId());
    validateVariableNameAndType(validation, entity, "xAxisVariable", pluginSpec.getXAxisVariable(), APIVariableType.NUMBER, APIVariableType.DATE);
    validateVariableNameAndType(validation, entity, "yAxisVariable", pluginSpec.getYAxisVariable(), APIVariableType.NUMBER, APIVariableType.DATE);
    if (pluginSpec.getFacetVariable() != null) {
      for (String facetVar : pluginSpec.getFacetVariable()) {
        validateVariableNameAndType(validation, entity, "facetVariable", facetVar, APIVariableType.STRING);
      }
    }
    return validation.build();
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(MosaicSpec pluginSpec) {
    StreamSpec spec = new StreamSpec(DATAFILE_NAME, pluginSpec.getEntityId());
    spec.add(pluginSpec.getXAxisVariable());
    spec.add(pluginSpec.getYAxisVariable());
    if (pluginSpec.getFacetVariable() != null) {
      spec.addAll(pluginSpec.getFacetVariable());
    }
    return new ListBuilder<StreamSpec>(spec).toList();
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      MosaicSpec spec = getPluginSpec();
      connection.voidEval("data <- fread('" + DATAFILE_NAME + "')");
      String facetVar1 = ((spec.getFacetVariable() == null) ? "" : spec.getFacetVariable().get(0));
      String facetVar2 = ((spec.getFacetVariable() == null) ? "" : spec.getFacetVariable().get(1));
      connection.voidEval("map <- data.frame("
          + "'id'=c('xAxisVariable', "
          + "       'yAxisVariable', "
          + "       'facetVariable1', "
          + "       'facetVariable2'), "
          + "'plotRef'=c('" + spec.getXAxisVariable()  + "'"
          + ", '" +           spec.getYAxisVariable()  + "'"
          + ", '" +           facetVar1 + "'"
          + ", '" +           facetVar2 + "', stringsAsFactors=FALSE))";
      String outFile = connection.eval("contingencyTable(data, map)").asString();
      RFileInputStream response = connection.openFile(outFile);
      IoUtil.transferStream(out, response);
      response.close();
      out.flush();
    });
  }
}