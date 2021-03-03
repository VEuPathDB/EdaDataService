package org.veupathdb.service.eda.ds.plugin;

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
import org.gusdb.fgputil.validation.ValidationException;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.veupathdb.service.eda.common.model.VariableSource;
import org.veupathdb.service.eda.generated.model.RecordCountPostRequest;
import org.veupathdb.service.eda.generated.model.RecordCountSpec;
import org.veupathdb.service.eda.common.client.StreamSpec;
import org.json.JSONObject;

public class RecordCountPlugin extends AbstractPlugin<RecordCountPostRequest, RecordCountSpec> {

  private static final String STREAM_NAME = "stream1";

  @Override
  protected Class<RecordCountSpec> getVisualizationSpecClass() {
    return RecordCountSpec.class;
  }

  @Override
  protected ValidationBundle validateVisualizationSpec(RecordCountSpec pluginSpec) throws ValidationException {
    ValidationBundleBuilder validation = ValidationBundle.builder(ValidationLevel.RUNNABLE);
    getValidEntity(validation, pluginSpec.getEntityId());
    return validation.build();
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(RecordCountSpec pluginSpec) {
    // only need one stream for the requested entity and no vars (IDs included automatically)
    StreamSpec spec = new StreamSpec(STREAM_NAME, pluginSpec.getEntityId())
        // add first var in entity to work around no-vars bug in subsetting service
        .addVariable(getReferenceMetadata().getEntity(pluginSpec.getEntityId()).stream()
            .filter(var -> VariableSource.NATIVE.equals(var.getSource()))
            .findFirst().orElseThrow()); // should have at least one native var
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
