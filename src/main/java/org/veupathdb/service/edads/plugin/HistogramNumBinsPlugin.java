package org.veupathdb.service.edads.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.List;
import java.util.Map;
import org.veupathdb.service.edads.generated.model.HistogramNumBinsPostRequest;
import org.veupathdb.service.edads.generated.model.HistogramNumBinsSpec;
import org.veupathdb.service.edads.util.StreamSpec;
import org.veupathdb.service.edads.util.HistogramPlugin;

public class HistogramNumBinsPlugin extends HistogramPlugin<HistogramNumBinsPostRequest, HistogramNumBinsSpec>{

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    HistogramSpec spec = getPluginSpec();
/*
//  TODO revisit simpleHistogram for numBins rather than binWidth
    EntityDef entity = new EntityDef(spec.getEntityId());
    VariableDef xVar = entity.get(spec.getXAxisVariable());
    APIVariableType xType = xVar.getType();
    
	boolean simpleHistogram = false;
    if (spec.getOverlayVariable() == null 
    		&& spec.getFacetVariable() == null
    		&& spec.getNumBins() != null
    		&& xType.equals(APIVariableType.NUMBER)
    		&& spec.getValueSpec().equals('count')
    		&& dataStreams.size() == 1) {
      simpleHistogram = true;
    }

	if (simpleHistogram) {
	  Integer numBins = spec.getNumBins().intValue();
	  //TODO calculate binWidth from numBins and range
	  Wrapper<Integer> rowCount = new Wrapper<>(0);
	  Scanner s = new Scanner(dataStreams.get(0)).useDelimiter("\n");
	  s.nextLine(); // ignore header, expecting single column representing ordered xVar values
	  Double binStart = s.nextLine().asDouble();
	  rowCount.set(1);
	  Double nextBinStart = binStart + binWidth;
	  
	  while(s.hasNextLine()) {
            Double val = s.nextLine().asDouble();
            if (val >= nextBinStart) {
              JSONObject histogram = new JSONObject;
              histogram.put("binLabel", "[" + binStart + " - " + nextBinStart + ")");
              histogram.put("binStart", binStart);
              histogram.put("value", rowCount);
              out.write(histogram.toString());
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
*/  
      useRConnectionWithRemoteFiles(dataStreams, connection -> {
        connection.voidEval("data <- fread(" + DATAFILE_NAME + ")");
        String[] variableNames = {"xAxisVariable",
       		  					  "overlayVariable",
  								  "facetVariable1",
  								  "facetVariable2"};
        String[] variables = {spec.getXAxisVariable(),
    		  				  spec.getOverlayVariable(),
        					  Array.get(spec.getFacetVariable(),0),
        					  Array.get(spec.getFacetVariable(),1)};
        RList plotRefMap = new RList(new REXP(variableNames), new REXP(variables))
        connection.assign("map", plotRefMap);
        connection.voidEval("names(map) <- c('id', 'plotRef')");
        Integer numBins = spec.getNumBins().intValue();
        connection.voidEval("x <- emptyStringToNull(map$id[map$plotRef == 'xAxisVariable'])");
        connection.voidEval("xRange <- max(data[[x]], na.rm=T) - min(data[[x]], na.rm=T)");
        connection.voidEval("binWidth <- xRange*1.01/" + numBins);
        String outFile = connection.eval("histogram(data, map, binWidth, " + spec.getValueSpec() + ")").asString();
        RFileInputStream response = connection.openFile(outFile);
        transferStream(response, out);
        response.close();
        out.flush();
	   });
//  }  
  }
}
