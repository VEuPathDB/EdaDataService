package org.veupathdb.service.eda.ds.plugin.pass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import org.gusdb.fgputil.IoUtil;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.generated.model.DateHistogramNumBinsPostRequest;
import org.veupathdb.service.eda.generated.model.DateHistogramNumBinsSpec;

import static org.veupathdb.service.eda.ds.util.RServeClient.useRConnectionWithRemoteFiles;

public class DateHistogramNumBinsPlugin extends HistogramPlugin<DateHistogramNumBinsPostRequest, DateHistogramNumBinsSpec>{

  @Override
  protected Class<DateHistogramNumBinsSpec> getVisualizationSpecClass() {
    return DateHistogramNumBinsSpec.class;
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    DateHistogramNumBinsSpec spec = getPluginSpec();
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
      if (spec.getViewportMin() != null & spec.getViewportMax() != null) {
        connection.voidEval("viewport <- list('xMin'='" + spec.getViewportMin() + "', 'xMax'='" + spec.getViewportMax() + "')");
      } else {
        connection.voidEval("viewport <- NULL");
      }
      if (spec.getNumBins() != null) {
        String numBins = spec.getNumBins().toString();
        connection.voidEval("x <- emptyStringToNull(map$id[map$plotRef == 'xAxisVariable'])");
        connection.voidEval("xVP <- adjustToViewport(data[[x]], viewport)");
        connection.voidEval("binWidth <- ceiling(as.numeric(diff(range(as.Date(xVP)))/" + numBins + "))");
        connection.voidEval("binWidth <- paste0(binWidth, ' days')");
      } else {
        connection.voidEval("binWidth <- NULL");
      }
      String outFile = connection.eval("histogram(data, map, binWidth, '" + spec.getValueSpec().toString().toLowerCase() + "', 'numBins', viewport)").asString();
      try (RFileInputStream response = connection.openFile(outFile)) {
        IoUtil.transferStream(out, response);
      }
      out.flush();
	 });
  }
}
