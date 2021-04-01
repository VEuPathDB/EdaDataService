package org.veupathdb.service.eda.ds.plugin.sample;

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
import org.json.JSONObject;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.VariableSource;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.RecordCountPostRequest;
import org.veupathdb.service.eda.generated.model.RecordCountSpec;

public class RecordCountPlugin extends AbstractPlugin<RecordCountPostRequest, RecordCountSpec> {

  @Override
  public String getDisplayName() {
    return "Record Count";
  }

  @Override
  public String getDescription() {
    return "Counts how many rows in a single stream of records";
  }

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
    StreamSpec spec = new StreamSpec(pluginSpec.getEntityId(), pluginSpec.getEntityId())
        // add first var in entity to work around no-vars bug in subsetting service
        .addVariable(getReferenceMetadata().getEntity(pluginSpec.getEntityId()).stream()
            .filter(var -> VariableSource.NATIVE.equals(var.getSource()))
            .findFirst().orElseThrow()); // should have at least one native var
    return new ListBuilder<StreamSpec>(spec).toList();
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    Wrapper<Integer> rowCount = new Wrapper<>(0);
    new Scanner(dataStreams.get(getPluginSpec().getEntityId()))
        .useDelimiter("\n")
        .forEachRemaining(str -> rowCount.set(rowCount.get() + 1));
    int recordCount = rowCount.get() - 1; // subtract 1 for header row
    out.write(new JSONObject().put("recordCount", recordCount).toString().getBytes());
    out.flush();
  }
}
