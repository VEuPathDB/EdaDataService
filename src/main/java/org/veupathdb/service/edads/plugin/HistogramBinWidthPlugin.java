package org.veupathdb.service.edads.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.Wrapper;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationException;
import org.json.JSONObject;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPList;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.veupathdb.service.edads.generated.model.APIVariableType;
import org.veupathdb.service.edads.generated.model.HistogramBinWidthPostRequest;
import org.veupathdb.service.edads.generated.model.HistogramBinWidthSpec;
import org.veupathdb.service.edads.plugin.HistogramPlugin;
import org.veupathdb.service.edads.util.EntityDef;
import org.veupathdb.service.edads.util.StreamSpec;
import org.veupathdb.service.edads.util.VariableDef;

public class HistogramBinWidthPlugin extends HistogramPlugin<HistogramBinWidthPostRequest, HistogramBinWidthSpec>{

  private static final String DATAFILE_NAME = "file1.txt";

  @Override
  protected Class<HistogramBinWidthSpec> getAnalysisSpecClass() {
    return HistogramBinWidthSpec.class;
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    HistogramBinWidthSpec spec = getPluginSpec();
    EntityDef entity = new EntityDef(spec.getEntityId());
    VariableDef xVar = entity.get(spec.getXAxisVariable());
    APIVariableType xType = xVar.getType();
    
	boolean simpleHistogram = false;
    if (spec.getOverlayVariable() == null 
    		&& spec.getFacetVariable() == null
    		&& spec.getBinWidth() != null
    		&& xType.equals(APIVariableType.NUMBER)
    		&& spec.getValueSpec().equals("count")
    		&& dataStreams.size() == 1) {
      simpleHistogram = true;
    }

   // start w just R
   simpleHistogram = false;
   // TODO revise as data will be ordered by id not by value. need min and max from steve
	if (simpleHistogram) {
	  Double binWidth = spec.getBinWidth().getNumericBinWidth().getType().doubleValue();
	  Wrapper<Integer> rowCount = new Wrapper<>(0);
	  Scanner s = new Scanner(dataStreams.get(0)).useDelimiter("\n");
	  s.nextLine(); // ignore header, expecting single column representing ordered xVar values
	  Double binStart = Double.valueOf(s.nextLine());
	  rowCount.set(1);
	  Double nextBinStart = binStart + binWidth;
	  
	  while(s.hasNextLine()) {
            Double val = Double.valueOf(s.nextLine());
            if (val >= nextBinStart) {
              JSONObject histogram = new JSONObject();
              histogram.put("binLabel", "[" + binStart + " - " + nextBinStart + ")");
              histogram.put("binStart", binStart);
              histogram.put("value", rowCount);
              out.write(histogram.toString().getBytes());
              binStart = nextBinStart;
              nextBinStart = nextBinStart + binWidth;
              rowCount.set(1);
            } else {
              rowCount.set(rowCount.get() + 1);
            }    
	  }
	  
	  s.close();
	  out.flush();
    } else {   
      useRConnectionWithRemoteFiles(dataStreams, connection -> {
        connection.voidEval("data <- fread('" + DATAFILE_NAME + "')");
        String overlayVar = ((spec.getOverlayVariable() == null) ? "" : spec.getOverlayVariable());
        String facetVar1 = ((spec.getFacetVariable() == null) ? "" : spec.getFacetVariable().get(0));
        String facetVar2 = ((spec.getFacetVariable() == null) ? "" : spec.getFacetVariable().get(1));
        connection.voidEval("map <- data.frame("
            + "'id'=c('xAxisVariable', "
            + "       'overlayVariable', "
            + "       'facetVariable1', "
            + "       'facetVariable2'), "
            + "'plotRef'=c('" + spec.getXAxisVariable() + "'"
            + ", '" +           overlayVar + "'"
            + ", '" +           facetVar1 + "'"
            + ", '" +           facetVar2 + "'), stringsAsFactors=FALSE)");
        String outFile = connection.eval("histogram(data, map, '" + spec.getBinWidth() + "', '" + spec.getValueSpec().toString().toLowerCase() + "')").asString();
        RFileInputStream response = connection.openFile(outFile);
        IoUtil.transferStream(out, response);
        response.close();
        out.flush();
	   });
    }  
  }
}
