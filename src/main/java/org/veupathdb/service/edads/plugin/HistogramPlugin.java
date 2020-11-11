package org.veupathdb.service.edads.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.List;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.veupathdb.service.edads.generated.model.HistogramPostRequest;
import org.veupathdb.service.edads.generated.model.HistogramSpec;
import org.veupathdb.service.edads.util.StreamSpec;
import org.veupathdb.service.edads.util.AbstractEdadsPlugin;

public class HistogramPlugin extends AbstractEdadsPlugin<HistogramPostRequest, HistogramSpec>{


  @Override
  protected Class<HistogramSpec> getConfigurationClass() {
    return HistogramSpec.class;
  }

  @Override
  protected ValidationBundle validateConfig(HistogramSpec pluginSpec) {
    return null;
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(HistogramSpec pluginSpec) {
    return null;
  }

  @Override
  protected void writeResults(OutputStream out, List<InputStream> dataStreams) throws IOException {

  }
}
