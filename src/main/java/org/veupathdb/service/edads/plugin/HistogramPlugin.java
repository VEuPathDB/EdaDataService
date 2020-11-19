package org.veupathdb.service.edads.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.List;
import java.util.Map;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.veupathdb.service.edads.generated.model.HistogramPostRequest;
import org.veupathdb.service.edads.generated.model.HistogramSpec;
import org.veupathdb.service.edads.util.StreamSpec;
import org.veupathdb.service.edads.util.AbstractEdadsPlugin;

public class HistogramPlugin extends AbstractEdadsPlugin<HistogramPostRequest, HistogramSpec>{

  @Override
  protected Class<HistogramSpec> getAnalysisSpecClass() {
    return HistogramSpec.class;
  }

  @Override
  protected ValidationBundle validateAnalysisSpec(HistogramSpec pluginSpec) {
    ValidationBundleBuilder validation = ValidationBundle.builder(ValidationLevel.RUNNABLE);
    EntityDef entity = getValidEntity(validation, pluginSpec.getEntityId());
    validateVariableNameAndType(validation, entity, "xAxisVariable", pluginSpec.getXAxisVariable(), APIVariableType.NUMBER, APIVariableType.DATE);
    validateVariableNameAndType(validation, entity, "overlayVariable", pluginSpec.getOverlayVariable(), APIVariableType.STRING);
    for (String facetVar : pluginSpec.getFacetVariable()) {
      validateVariableName(validation, entity, "facetVariable", facetVar, APIVariableType.STRING);
    }
    return validation.build();
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(HistogramSpec pluginSpec) {
    StreamSpec spec = new StreamSpec(DATAFILE_NAME, pluginSpec.getEntityId());
    spec.add(pluginSpec.getXAxisVariable());
    spec.add(pluginSpec.getOverlayVariable());
    spec.addAll(pluginSpec.getFacetVariable());
    return new ListBuilder<StreamSpec>(spec).toList();
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      ScatterplotSpec spec = getPluginSpec();
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
      String response = connection.eval("histogram(data, map, " + spec.getBinWidth() + ", " + spec.getValueSpec() + ")").asString();
      // TODO
      out.write(response.asString().getBytes());
	});
  }
}
