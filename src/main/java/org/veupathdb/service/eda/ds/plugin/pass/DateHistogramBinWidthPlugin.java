package org.veupathdb.service.eda.ds.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import org.gusdb.fgputil.IoUtil;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.generated.model.DateHistogramBinWidthPostRequest;
import org.veupathdb.service.eda.generated.model.DateHistogramBinWidthSpec;

import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class DateHistogramBinWidthPlugin extends HistogramPlugin<DateHistogramBinWidthPostRequest, DateHistogramBinWidthSpec>{

  @Override
  protected Class<DateHistogramBinWidthSpec> getVisualizationSpecClass() {
    return DateHistogramBinWidthSpec.class;
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    DateHistogramBinWidthSpec spec = getPluginSpec();
    EntityDef entity = getReferenceMetadata().getEntity(spec.getOutputEntityId());
    String xVar = toColNameOrEmpty(spec.getXAxisVariable());
    String overlayVar = toColNameOrEmpty(spec.getOverlayVariable());
    String facetVar1 = spec.getFacetVariable() != null ? toColNameOrEmpty(spec.getFacetVariable().get(0)) : "";
    String facetVar2 = spec.getFacetVariable() != null ? toColNameOrEmpty(spec.getFacetVariable().get(1)) : "";
    // TODO eventually varId and entityId will be a single string delimited by '.'
    String xVarEntity = spec.getXAxisVariable() != null ? spec.getXAxisVariable().getEntityId() : "";
    String overlayEntity = spec.getOverlayVariable() != null ? spec.getOverlayVariable().getEntityId() : "";
    String facetEntity1 = spec.getFacetVariable() != null ? spec.getFacetVariable().get(0).getEntityId() : "";
    String facetEntity2 = spec.getFacetVariable() != null ? spec.getFacetVariable().get(1).getEntityId() : "";
    // TODO this only works for now bc outputEntityId must be the same as var entityId
    String xVarType = spec.getXAxisVariable() != null ? entity.getVariable(spec.getXAxisVariable()).getType().toString() : "";
    String overlayType = spec.getOverlayVariable() != null ? entity.getVariable(spec.getOverlayVariable()).getType().toString() : "";
    String facetType1 = spec.getFacetVariable() != null ? entity.getVariable(spec.getFacetVariable().get(0)).getType().toString() : "";
    String facetType2 = spec.getFacetVariable() != null ? entity.getVariable(spec.getFacetVariable().get(1)).getType().toString() : "";
    
    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      connection.voidEval("data <- fread('" + DATAFILE_NAME + "', na.strings=c(''))");
      connection.voidEval("map <- data.frame("
          + "'plotRef'=c('xAxisVariable', "
          + "       'overlayVariable', "
          + "       'facetVariable1', "
          + "       'facetVariable2'), "
          + "'id'=c('" + xVar + "'"
          + ", '" + overlayVar + "'"
          + ", '" + facetVar1 + "'"
          + ", '" + facetVar2 + "'), "
          + "'entityId'=c('" + xVarEntity + "'"
          + ", '" + overlayEntity + "'"
          + ", '" + facetEntity1 + "'"
          + ", '" + facetEntity2 + "'), "
          + "'dataType'=c('" + xVarType + "'"
          + ", '" + overlayType + "'"
          + ", '" + facetType1 + "'"
          + ", '" + facetType2 + "'), stringsAsFactors=FALSE)");
      String binWidth = spec.getBinWidth() == null ? "NULL" : "'" + spec.getBinWidth() + "'";
      if (spec.getViewportMin() != null & spec.getViewportMax() != null) {
        connection.voidEval("viewport <- list('x.min'='" + spec.getViewportMin() + "', 'x.max'='" + spec.getViewportMax() + "')");
      } else {
        connection.voidEval("viewport <- NULL");
      }
      String outFile = connection.eval("histogram(data, map, " +
          binWidth + ", '" +
          spec.getValueSpec().toString().toLowerCase() + "', 'binWidth', viewport)").asString();
      try (RFileInputStream response = connection.openFile(outFile)) {
        IoUtil.transferStream(out, response);
      }
      out.flush();
	 });  
  }
}
