package org.veupathdb.service.edads.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.rosuda.REngine.REXP;
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
    for (String facetVar : pluginSpec.getFacetVariable()) {
      validateVariableName(validation, entity, "facetVariable", facetVar, APIVariableType.STRING);
    }
    return validation.build();
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(MosaicSpec pluginSpec) {
    StreamSpec spec = new StreamSpec(DATAFILE_NAME, pluginSpec.getEntityId());
    spec.add(pluginSpec.getXAxisVariable());
    spec.add(pluginSpec.getYAxisVariable());
    spec.addAll(pluginSpec.getFacetVariable());
    return new ListBuilder<StreamSpec>(spec).toList();
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    useRConnectionWithRemoteFiles(dataStreams, connection -> {
      MosaicSpec spec = getPluginSpec();
      connection.voidEval("data <- fread(" + DATAFILE_NAME + ")");
      String[] variableNames = {"xAxisVariable",
								"yAxisVariable",
								"facetVariable1",
								"facetVariable2"};
      String[] variables = {spec.getXAxisVariable(),
    		  				spec.getYAxisVariable(),
    		  				Array.get(spec.getFacetVariable(),0),
    		  				Array.get(spec.getFacetVariable(),1)};
      RList plotRefMap = new RList(new REXP(variableNames), new REXP(variables))
      connection.assign("map", plotRefMap);
      connection.voidEval("names(map) <- c('id', 'plotRef')");
      String response = connection.eval("contingencyTable(data, map)").asString();
      // TODO
      out.write(response.asString().getBytes());
    });
  }
}