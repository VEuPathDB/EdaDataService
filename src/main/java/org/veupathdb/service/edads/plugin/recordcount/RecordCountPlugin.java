package org.veupathdb.service.edads.plugin.recordcount;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import java.util.List;
import java.util.Map;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.veupathdb.service.edads.generated.model.RecordCountPostRequest;
import org.veupathdb.service.edads.generated.model.RecordCountSpec;
import org.veupathdb.service.edads.model.EntityDef;
import org.veupathdb.service.edads.model.StreamSpec;
import org.veupathdb.service.edads.plugin.AbstractEdadsPlugin;

public class RecordCountPlugin extends AbstractEdadsPlugin<RecordCountPostRequest, RecordCountSpec> {


  @Override
  protected Class<RecordCountSpec> getConfigurationClass() {
    return RecordCountSpec.class;
  }

  @Override
  protected ValidationBundle validate(RecordCountSpec pluginSpec, Map<String, EntityDef> entities) {
    return null;
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(RecordCountSpec pluginSpec, Map<String, EntityDef> supplementedEntities) {
    return null;
  }

  @Override
  protected void writeResults(OutputStream out, List<InputStream> dataStreams) throws IOException {

  }
}
