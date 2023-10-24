package org.veupathdb.service.eda.data.plugin.pass;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.DelimitedDataParser;
import org.gusdb.fgputil.validation.ValidationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.data.core.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.data.metadata.AppsMetadata;
import org.veupathdb.service.eda.generated.model.TablePostRequest;
import org.veupathdb.service.eda.generated.model.TableSpec;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.FormatUtil.TAB;

public class TablePlugin extends AbstractEmptyComputePlugin<TablePostRequest, TableSpec> {

  private static final Logger LOG = LogManager.getLogger(TablePlugin.class);

  @Override
  public String getDisplayName() {
    return "Table";
  }

  @Override
  public String getDescription() {
    return "Visualize a table of requested variables";
  }
  
  @Override
  public List<String> getProjects() {
    return List.of(AppsMetadata.CLINEPI_PROJECT, AppsMetadata.MICROBIOME_PROJECT);
  }

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new EmptyComputeClassGroup(TablePostRequest.class, TableSpec.class);
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .pattern()
        .element("outputVariableIds")
      .done();
  }

  @Override
  protected void validateVisualizationSpec(TableSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("outputVariableIds", pluginSpec.getOutputVariable()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(TableSpec pluginSpec) {
    return List.of(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVars(pluginSpec.getOutputVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    BufferedWriter bufOut = new BufferedWriter(new OutputStreamWriter(out));
    //get paging config
    TableSpec spec = getPluginSpec();
    Long numRows = spec.getPagingConfig().getNumRows();
    Long offset = spec.getPagingConfig().getOffset();
    
    // create scanner and line parser
    Scanner s = new Scanner(dataStreams.get(DEFAULT_SINGLE_STREAM_NAME)).useDelimiter(NL);
    DelimitedDataParser parser = new DelimitedDataParser(s.nextLine(), TAB, true);

    //print header
    List<String> header = parser.getColumnNames();
    bufOut.write("{\"columns\":[");
    boolean first = true;
    for (String colName : header) {
      if (first) first = false; else bufOut.write(",");
      LOG.debug("col name: " + colName);
      String[] varSpec = colName.split("\\.");
      LOG.debug(String.join(",",varSpec));
      bufOut.write(new JSONObject()
          .put("entityId", varSpec[0])
          .put("variableId", varSpec[1])
          .toString()
        );
    }
    bufOut.write("],\"rows\":[");
    
    //loop through and print data
    first = true;
    int offsetCount = 0;
    int rowCount = 0;
    while (s.hasNextLine()) {
      if (offset != null) {
        if (offsetCount < offset) {
          offsetCount++;
          s.nextLine();
        } else {
          if (numRows != null) {
            if (rowCount < numRows) {
              rowCount++;
              String[] row = parser.parseLineToArray(s.nextLine());
              if (first) first = false; else bufOut.write(",");
              bufOut.write(new JSONArray(row).toString());
            } else {
              break;
            }
          } else {
            String[] row = parser.parseLineToArray(s.nextLine());
            if (first) first = false; else bufOut.write(",");
            bufOut.write(new JSONArray(row).toString());
          }
        }
      }
    }
    
    // close array and enclosing object
    bufOut.write("]}");
    bufOut.flush();
  }
}
