package org.veupathdb.service.eda.ds.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Scanner;
import org.gusdb.fgputil.IoUtil;
import org.json.JSONObject;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.DateHistogramBinWidthPostRequest;
import org.veupathdb.service.eda.generated.model.DateHistogramBinWidthSpec;
import org.veupathdb.service.eda.generated.model.ValueSpec;
import org.veupathdb.service.eda.ds.util.EntityDef;
import org.veupathdb.service.eda.ds.util.VariableDef;

public class DateHistogramBinWidthPlugin extends HistogramPlugin<DateHistogramBinWidthPostRequest, DateHistogramBinWidthSpec>{

  @Override
  protected Class<DateHistogramBinWidthSpec> getAnalysisSpecClass() {
    return DateHistogramBinWidthSpec.class;
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    DateHistogramBinWidthSpec spec = getPluginSpec();
    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      connection.voidEval("data <- fread('" + DATAFILE_NAME + "', na.strings=c(''))");
      String facetVar1 = spec.getFacetVariable() != null ? toColNameOrEmpty(spec.getFacetVariable().get(0)) : "";
      String facetVar2 = spec.getFacetVariable() != null ? toColNameOrEmpty(spec.getFacetVariable().get(1)) : "";
      // NOTE: eventually varId and entityId will be a single string delimited by '.'
      String xAxisEntity = spec.getXAxisVariable() != null ? spec.getXAxisVariable().getEntityId() : "";
      String overlayEntity = spec.getXAxisVariable() != null ? spec.getXAxisVariable().getEntityId() : "";
      String facetEntity1 = spec.getXAxisVariable() != null ? spec.getXAxisVariable().getEntityId() : "";
      String facetEntity2 = spec.getXAxisVariable() != null ? spec.getXAxisVariable().getEntityId() : "";
      connection.voidEval("map <- data.frame("
          + "'plotRef'=c('xAxisVariable', "
          + "       'overlayVariable', "
          + "       'facetVariable1', "
          + "       'facetVariable2'), "
          + "'id'=c('" + toColNameOrEmpty(spec.getXAxisVariable()) + "'"
          + ", '" + toColNameOrEmpty(spec.getOverlayVariable()) + "'"
          + ", '" + facetVar1 + "'"
          + ", '" + facetVar2 + "'), "
          + "'entityId'=c('" + xAxisEntity + "'"
          + ", '" + overlayEntity + "'"
          + ", '" + facetEntity1 + "'"
          + ", '" + facetEntity2 + "'), stringsAsFactors=FALSE)");
      String binWidth = spec.getBinWidth() == null ? "NULL" : "'" + spec.getBinWidth() + "'";
      if (spec.getViewportMin() != null & spec.getViewportMax() != null) {
        connection.voidEval("viewport <- list('min'='" + spec.getViewportMin() + "', 'max'='" + spec.getViewportMax() + "')");
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
