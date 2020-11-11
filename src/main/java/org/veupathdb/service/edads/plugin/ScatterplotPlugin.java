package org.veupathdb.service.edads.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.List;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.veupathdb.service.edads.generated.model.ScatterplotPostRequest;
import org.veupathdb.service.edads.generated.model.ScatterplotSpec;
import org.veupathdb.service.edads.util.StreamSpec;
import org.veupathdb.service.edads.util.AbstractEdadsPlugin;

public class ScatterplotPlugin extends AbstractEdadsPlugin<ScatterplotPostRequest, ScatterplotSpec> {

  @Override
  protected Class<ScatterplotSpec> getConfigurationClass() {
    return ScatterplotSpec.class;
  }

  @Override
  protected ValidationBundle validateConfig(ScatterplotSpec pluginSpec) {
    return null;
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(ScatterplotSpec pluginSpec) {
    return null;
  }

  @Override
  protected void writeResults(OutputStream out, List<InputStream> dataStreams) throws IOException {

  }
}