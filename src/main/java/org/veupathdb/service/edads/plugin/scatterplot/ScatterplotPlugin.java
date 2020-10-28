package org.veupathdb.service.edads.plugin.scatterplot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.List;
import java.util.Map;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.veupathdb.service.edads.generated.model.ScatterplotPostRequest;
import org.veupathdb.service.edads.generated.model.ScatterplotSpec;
import org.veupathdb.service.edads.util.EntityDef;
import org.veupathdb.service.edads.util.StreamSpec;
import org.veupathdb.service.edads.plugin.AbstractEdadsPlugin;

public class ScatterplotPlugin extends AbstractEdadsPlugin<ScatterplotPostRequest, ScatterplotSpec> {

  @Override
  protected Class<ScatterplotSpec> getConfigurationClass() {
    return ScatterplotSpec.class;
  }

  @Override
  protected ValidationBundle validate(ScatterplotSpec pluginSpec, Map<String, EntityDef> entities) {
    return null;
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(ScatterplotSpec pluginSpec, Map<String, EntityDef> supplementedEntities) {
    return null;
  }

  @Override
  protected void writeResults(OutputStream out, List<InputStream> dataStreams) throws IOException {

  }
}
