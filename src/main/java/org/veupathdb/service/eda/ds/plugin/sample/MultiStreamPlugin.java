package org.veupathdb.service.eda.ds.plugin.sample;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.stream.Collectors;
import org.gusdb.fgputil.DelimitedDataParser;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationBundle.ValidationBundleBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.common.model.VariableSource;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.RecordCountPostRequest;
import org.veupathdb.service.eda.generated.model.RecordCountSpec;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.FormatUtil.TAB;
import static org.gusdb.fgputil.FormatUtil.join;
import static org.gusdb.fgputil.functional.Functions.getMapFromKeys;

public class MultiStreamPlugin extends AbstractPlugin<RecordCountPostRequest, RecordCountSpec> {

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

  /**
   * Request three streams of the same entity, one for each of the first three native
   * vars on that entity.  The result writer will merge the streams back together into
   * a tabular result.  The purpose of this plugin is to test the multiple parallel
   * streams code and also to initially test the pass-through stream ability of the
   * merging service.  Thus the header should show dot notation for var names.
   *
   * @param pluginSpec client-defined plugin spec
   * @return list of streams this plugin needs to display its data
   */
  @Override
  protected List<StreamSpec> getRequestedStreams(RecordCountSpec pluginSpec) {
    // get the first three native vars of this entity
    EntityDef entity = getReferenceMetadata().getEntity(pluginSpec.getEntityId());
    List<VariableDef> varsToRequest = getVars(entity);
    // ask for up to three streams, named for the vars they will provide
    return varsToRequest.stream()
      .map(var -> new StreamSpec(toColNameOrEmpty(var), entity.getId()).addVariable(var))
      .collect(Collectors.toList());
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    EntityDef entity = getReferenceMetadata().getEntity(getPluginSpec().getEntityId());
    String idColumn = entity.getIdColumnName();
    try (Writer writer = new BufferedWriter(new OutputStreamWriter(out))) {
      // write header
      writer.write(idColumn + TAB + join(dataStreams.keySet(), TAB) + NL);
      Map<String, Scanner> scannerMap = getMapFromKeys(dataStreams.keySet(),
          key -> new Scanner(dataStreams.get(key)));
      Map<String, DelimitedDataParser> parserMap = getMapFromKeys(dataStreams.keySet(),
          key -> new DelimitedDataParser(scannerMap.get(key).nextLine(), TAB, true));
      while(scannerMap.values().iterator().next().hasNextLine()) {
        boolean isFirst = true;
        for (Entry<String,Scanner> stream : scannerMap.entrySet()) {
          Map<String,String> row = parserMap.get(stream.getKey()).parseLine(stream.getValue().nextLine());
          if (isFirst) {
            writer.write(row.get(idColumn));
            isFirst = false;
          }
          writer.write(TAB);
          writer.write(row.get(stream.getKey()));
        }
        writer.write(NL);
      }
      writer.flush();
    }
  }

  private static List<VariableDef> getVars(EntityDef entity) {
    List<VariableDef> varsToRequest = new ArrayList<>();
    for (VariableDef var : entity) {
      if (VariableSource.NATIVE.equals(var.getSource())) {
        varsToRequest.add(var);
      }
      // no more than three
      if (varsToRequest.size() == 3) break;
    }
    return varsToRequest;
  }
}
