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
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.LineplotPostRequest;
import org.veupathdb.service.eda.generated.model.LineplotSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class LineplotPlugin extends AbstractPlugin<LineplotPostRequest, LineplotSpec> {

  private static final String DATAFILE_NAME = "file1.txt";

  @Override
  protected Class<LineplotSpec> getVisualizationSpecClass() {
    return LineplotSpec.class;
  }

  @Override
  protected ValidationBundle validateVisualizationSpec(LineplotSpec pluginSpec) throws ValidationException {
    ValidationBundleBuilder validation = ValidationBundle.builder(ValidationLevel.RUNNABLE);
    EntityDef entity = getValidEntity(validation, pluginSpec.getOutputEntityId());
    validateVariableNameAndType(validation, entity, "xAxisVariable", pluginSpec.getXAxisVariable(), APIVariableType.NUMBER, APIVariableType.DATE);
    validateVariableNameAndType(validation, entity, "yAxisVariable", pluginSpec.getYAxisVariable(), APIVariableType.NUMBER, APIVariableType.DATE);
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
  protected List<StreamSpec> getRequestedStreams(LineplotSpec pluginSpec) {
    StreamSpec spec = new StreamSpec(DATAFILE_NAME, pluginSpec.getOutputEntityId());
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
    LineplotSpec spec = getPluginSpec();
    EntityDef entity = getReferenceMetadata().getEntity(spec.getOutputEntityId());
    String xVar = toColNameOrEmpty(spec.getXAxisVariable());
    String yVar = toColNameOrEmpty(spec.getYAxisVariable());
    String overlayVar = toColNameOrEmpty(spec.getOverlayVariable());
    String facetVar1 = spec.getFacetVariable() != null ? toColNameOrEmpty(spec.getFacetVariable().get(0)) : "";
    String facetVar2 = spec.getFacetVariable() != null ? toColNameOrEmpty(spec.getFacetVariable().get(1)) : "";
    String xVarEntity = spec.getXAxisVariable() != null ? spec.getXAxisVariable().getEntityId() : "";
    String yVarEntity = spec.getYAxisVariable() != null ? spec.getYAxisVariable().getEntityId() : "";
    String overlayEntity = spec.getOverlayVariable() != null ? spec.getOverlayVariable().getEntityId() : "";
    String facetEntity1 = spec.getFacetVariable() != null ? spec.getFacetVariable().get(0).getEntityId() : "";
    String facetEntity2 = spec.getFacetVariable() != null ? spec.getFacetVariable().get(1).getEntityId() : "";
    String xVarType = spec.getXAxisVariable() != null ? entity.getVariable(spec.getXAxisVariable()).getType().toString() : "";
    String yVarType = spec.getYAxisVariable() != null ? entity.getVariable(spec.getYAxisVariable()).getType().toString() : "";
    String overlayType = spec.getOverlayVariable() != null ? entity.getVariable(spec.getOverlayVariable()).getType().toString() : "";
    String facetType1 = spec.getFacetVariable() != null ? entity.getVariable(spec.getFacetVariable().get(0)).getType().toString() : "";
    String facetType2 = spec.getFacetVariable() != null ? entity.getVariable(spec.getFacetVariable().get(1)).getType().toString() : "";
    
    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      connection.voidEval("data <- fread('" + DATAFILE_NAME + "', na.strings=c(''))");
      connection.voidEval("map <- data.frame("
            + "'plotRef'=c('xAxisVariable', "
            + "       'yAxisVariable', "
            + "       'overlayVariable', "
            + "       'facetVariable1', "
            + "       'facetVariable2'), "
            + "'id'=c('" + xVar + "'"
            + ", '" + yVar + "'"
            + ", '" + overlayVar + "'"
            + ", '" + facetVar1 + "'"
            + ", '" + facetVar2 + "'), "
            + "'entityId'=c('" + xVarEntity + "'"
            + ", '" + yVarEntity + "'"
            + ", '" + overlayEntity + "'"
            + ", '" + facetEntity1 + "'"
            + ", '" + facetEntity2 + "'), "
            + "'dataType'=c('" + xVarType + "'"
            + ", '" + yVarType + "'"
            + ", '" + overlayType + "'"
            + ", '" + facetType1 + "'"
            + ", '" + facetType2 + "'), stringsAsFactors=FALSE)");
      String outFile = connection.eval("scattergl(data, map, 'raw')").asString();
      try (RFileInputStream response = connection.openFile(outFile)) {
        IoUtil.transferStream(out, response);
      }
      out.flush();
    }); 
  }
}
