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
import org.veupathdb.service.edads.generated.model.RecordCountPostRequest;
import org.veupathdb.service.edads.generated.model.RecordCountSpec;
import org.veupathdb.service.edads.util.StreamSpec;
import org.veupathdb.service.edads.util.AbstractEdadsPlugin;
import org.json.JSONObject;

public class RecordCountPlugin extends AbstractEdadsPlugin<RecordCountPostRequest, RecordCountSpec> {

  private static final String STREAM_NAME = "stream1";

  @Override
  protected Class<RecordCountSpec> getAnalysisSpecClass() {
    return RecordCountSpec.class;
  }

  @Override
  protected ValidationBundle validateAnalysisSpec(RecordCountSpec pluginSpec) {
    ValidationBundleBuilder validation = ValidationBundle.builder(ValidationLevel.RUNNABLE);
    if (!getEntityMap().containsKey(pluginSpec.getEntityId())) {
      validation.addError("No entity exists in study '" + getStudyId() + "' with ID '" + pluginSpec.getEntityId() + "'.");
    }
    return validation.build();
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(RecordCountSpec pluginSpec) {
    // only need one stream for the requested entity and no vars (IDs included automatically)
    StreamSpec spec = new StreamSpec(STREAM_NAME, pluginSpec.getEntityId())
        // add first var in entity to work around no-vars bug in subsetting service
        .addVariable(getEntityMap().get(pluginSpec.getEntityId()).get(0));
    return new ListBuilder<StreamSpec>(spec).toList();
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    Wrapper<Integer> rowCount = new Wrapper<>(0);
    new Scanner(dataStreams.get(STREAM_NAME))
        .useDelimiter("\n")
        .forEachRemaining(str -> rowCount.set(rowCount.get() + 1));
    int recordCount = rowCount.get() - 1; // subtract 1 for header row
    out.write(new JSONObject().put("recordCount", recordCount).toString().getBytes());
    out.flush();
  }
}
