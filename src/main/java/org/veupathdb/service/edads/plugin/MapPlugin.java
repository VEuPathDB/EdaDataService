package org.veupathdb.service.edads.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.Wrapper;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.veupathdb.service.edads.generated.model.MapPostRequest;
import org.veupathdb.service.edads.generated.model.MapSpec;
import org.veupathdb.service.edads.util.StreamSpec;
import org.veupathdb.service.edads.util.AbstractEdadsPlugin;
import org.json.JSONObject;

public class MapPlugin extends AbstractEdadsPlugin<MapPostRequest, MapSpec> {

  @Override
  protected Class<MapSpec> getAnalysisSpecClass() {
    return MapSpec.class;
  }

  @Override
  protected ValidationBundle validateAnalysisSpec(MapSpec pluginSpec) {
    ValidationBundleBuilder validation = ValidationBundle.builder(ValidationLevel.RUNNABLE);
    if (!getEntityMap().containsKey(pluginSpec.getEntityId())) {
      validation.addError("No entity exists in study '" + getStudyId() + "' with ID '" + pluginSpec.getEntityId() + "'.");
    }
    return validation.build();
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(MapSpec pluginSpec) {
    // only need one stream for the requested entity and no vars (IDs included automatically)
    return new ListBuilder<StreamSpec>(new StreamSpec("stream1", pluginSpec.getEntityId())).toList();
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    //TODO
  }
}
