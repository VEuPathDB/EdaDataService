package org.veupathdb.service.edads.plugin.histogram;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.List;
import java.util.Map;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.veupathdb.service.edads.generated.model.HistogramPostRequest;
import org.veupathdb.service.edads.generated.model.HistogramSpec;
import org.veupathdb.service.edads.model.EntityDef;
import org.veupathdb.service.edads.model.StreamSpec;
import org.veupathdb.service.edads.plugin.AbstractEdadsPlugin;

public class HistogramPlugin extends AbstractEdadsPlugin<HistogramPostRequest, HistogramSpec>{


  @Override
  protected Class<HistogramSpec> getConfigurationClass() {
    return HistogramSpec.class;
  }

  @Override
  protected ValidationBundle validate(HistogramSpec pluginSpec, Map<String, EntityDef> entities) {
    return null;
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(HistogramSpec pluginSpec, Map<String, EntityDef> supplementedEntities) {
    return null;
  }

  @Override
  protected void writeResults(OutputStream out, List<InputStream> dataStreams) throws IOException {

  }
}
