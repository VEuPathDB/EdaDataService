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
import java.util.Scanner;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.DelimitedDataParser;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.common.model.VariableSource;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.MultiStreamPostRequest;
import org.veupathdb.service.eda.generated.model.MultiStreamSpec;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.FormatUtil.TAB;
import static org.gusdb.fgputil.FormatUtil.join;
import static org.gusdb.fgputil.functional.Functions.getMapFromKeys;

public class MultiStreamPlugin extends AbstractPlugin<MultiStreamPostRequest, MultiStreamSpec> {

  private static final Logger LOG = LogManager.getLogger(MultiStreamPlugin.class);

  @Override
  public String getDisplayName() {
    return "Multiple Stream Combiner";
  }

  @Override
  public String getDescription() {
    return "Merges streams for up to three vars into a single tabular output and dumps it";
  }

  @Override
  protected Class<MultiStreamSpec> getVisualizationSpecClass() {
    return MultiStreamSpec.class;
  }

  @Override
  protected void validateVisualizationSpec(MultiStreamSpec pluginSpec) throws ValidationException {
    getReferenceMetadata().getEntity(pluginSpec.getEntityId())
        .orElseThrow(() -> new ValidationException("Invalid entity ID: " + pluginSpec.getEntityId()));
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
  protected List<StreamSpec> getRequestedStreams(MultiStreamSpec pluginSpec) {
    // get the first three native vars of this entity
    EntityDef entity = getReferenceMetadata().getEntity(pluginSpec.getEntityId()).orElseThrow();
    List<VariableDef> varsToRequest = getVars(entity);
    // ask for up to three streams, named for the vars they will provide
    return varsToRequest.stream()
      .map(var -> new StreamSpec(toColNameOrEmpty(var), entity.getId()).addVar(var))
      .collect(Collectors.toList());
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    EntityDef entity = getReferenceMetadata().getEntity(getPluginSpec().getEntityId()).orElseThrow();
    String idColumn = toColNameOrEmpty(entity.getIdColumnDef());
    try (Writer writer = new BufferedWriter(new OutputStreamWriter(out))) {
      // write header
      writer.write(idColumn + TAB + join(dataStreams.keySet(), TAB) + NL);
      Map<String, Scanner> scannerMap = getMapFromKeys(dataStreams.keySet(),
          key -> new Scanner(dataStreams.get(key)));
      Map<String, DelimitedDataParser> parserMap = getMapFromKeys(dataStreams.keySet(),
          key -> new DelimitedDataParser(scannerMap.get(key).nextLine(), TAB, true));
      Scanner firstScanner = scannerMap.values().iterator().next();
      while(firstScanner.hasNextLine()) {
        boolean isFirst = true;
        for (String streamName : scannerMap.keySet()) {
          Map<String,String> row = parserMap.get(streamName)
              .parseLine(scannerMap.get(streamName).nextLine());
          if (isFirst) {
            writer.write(row.get(idColumn));
            isFirst = false;
          }
          writer.write(TAB);
          writer.write(row.get(streamName));
        }
        writer.write(NL);
      }
      writer.flush();
    }
  }

  private static List<VariableDef> getVars(EntityDef entity) {
    List<VariableDef> varsToRequest = new ArrayList<>();
    for (VariableDef var : entity.getVariablesWithDefaultUnitsAndScale()) {
      if (VariableSource.NATIVE.equals(var.getSource())) {
        varsToRequest.add(var);
      }
      // no more than three
      if (varsToRequest.size() == 3) break;
    }
    return varsToRequest;
  }
}
