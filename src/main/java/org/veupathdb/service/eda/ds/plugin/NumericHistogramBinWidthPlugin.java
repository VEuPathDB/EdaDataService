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
import org.veupathdb.service.eda.generated.model.NumericHistogramBinWidthPostRequest;
import org.veupathdb.service.eda.generated.model.NumericHistogramBinWidthSpec;
import org.veupathdb.service.eda.generated.model.ValueSpec;
import org.veupathdb.service.eda.ds.util.EntityDef;
import org.veupathdb.service.eda.ds.util.VariableDef;

public class NumericHistogramBinWidthPlugin extends HistogramPlugin<NumericHistogramBinWidthPostRequest, NumericHistogramBinWidthSpec>{

  @Override
  protected Class<NumericHistogramBinWidthSpec> getAnalysisSpecClass() {
    return NumericHistogramBinWidthSpec.class;
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    NumericHistogramBinWidthSpec spec = getPluginSpec();
    EntityDef entity = getEntityMap().get(spec.getEntityId());
    VariableDef xVar = entity.getVariable(spec.getXAxisVariable());
    APIVariableType xType = xVar.getType();

    boolean simpleHistogram = false;
    if (spec.getOverlayVariable() == null 
        && spec.getFacetVariable() == null
        && spec.getBinWidth() != null
        && xType.equals(APIVariableType.NUMBER)
        && spec.getValueSpec().equals(ValueSpec.COUNT)
        && dataStreams.size() == 1) {
      simpleHistogram = true;
    }

    // start w just R
    simpleHistogram = false;
    // TODO revise as data will be ordered by id not by value. need min and max from steve
    if (simpleHistogram) {
      Double binWidth = spec.getBinWidth().doubleValue();
      int rowCount = 0;
      Scanner s = new Scanner(dataStreams.get(DATAFILE_NAME)).useDelimiter("\n");
      s.nextLine(); // ignore header, expecting single column representing ordered xVar values
      Double binStart = Double.valueOf(s.nextLine());
      rowCount = 1;
      Double nextBinStart = binStart + binWidth;

      while(s.hasNextLine()) {
        double val = Double.valueOf(s.nextLine());
        if (val >= nextBinStart) {
          JSONObject histogram = new JSONObject();
          histogram.put("binLabel", "[" + binStart + " - " + nextBinStart + ")");
          histogram.put("binStart", binStart);
          histogram.put("value", rowCount);
          out.write(histogram.toString().getBytes());
          binStart = nextBinStart;
          nextBinStart = nextBinStart + binWidth;
          rowCount = 1;
        } else {
          rowCount++;
        }
      }
      out.flush();
    }
    else {
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
}
