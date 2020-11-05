package org.veupathdb.service.edads.plugin.recordcount;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.List;
import java.util.Scanner;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.Wrapper;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.veupathdb.service.edads.generated.model.RecordCountPostRequest;
import org.veupathdb.service.edads.generated.model.RecordCountSpec;
import org.veupathdb.service.edads.util.StreamSpec;
import org.veupathdb.service.edads.plugin.AbstractEdadsPlugin;
import org.json.JSONObject;

public class RecordCountPlugin extends AbstractEdadsPlugin<RecordCountPostRequest, RecordCountSpec> {

  @Override
  protected Class<RecordCountSpec> getConfigurationClass() {
    return RecordCountSpec.class;
  }

  @Override
  protected ValidationBundle validateConfig(RecordCountSpec pluginSpec) {
    ValidationBundleBuilder validation = ValidationBundle.builder(ValidationLevel.RUNNABLE);
    if (!getEntityMap().containsKey(pluginSpec.getEntityId())) {
      validation.addError("No entity exists on study '" + getStudyId() + "' with ID '" + pluginSpec.getEntityId() + "'.");
    }
    return validation.build();
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(RecordCountSpec pluginSpec) {
    String pkVarName = getEntityMap().get(pluginSpec.getEntityId()).get(0).getId();
    return new ListBuilder<StreamSpec>(new StreamSpec(pluginSpec.getEntityId()).addVariable(pkVarName)).toList();
  }

  @Override
  protected void writeResults(OutputStream out, List<InputStream> dataStreams) throws IOException {
    Wrapper<Integer> count = new Wrapper<>(0);
    new Scanner(dataStreams.get(0)).useDelimiter("\n").forEachRemaining(str -> count.set(count.get()+1));
    out.write(new JSONObject().put("recordCount", count).toString().getBytes());
    out.flush();
  }
}
