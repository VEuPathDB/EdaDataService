package org.veupathdb.service.eda.ds.plugin.pass;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.function.Function;
import org.gusdb.fgputil.DelimitedDataParser;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.constraints.DataElementSet;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.TablePostRequest;
import org.veupathdb.service.eda.generated.model.TableSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.FormatUtil.TAB;

public class TablePlugin extends AbstractPlugin<TablePostRequest, TableSpec> {

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
    return Arrays.asList("ClinEpiDB", "MicrobiomeDB");
  }

  @Override
  protected Class<TableSpec> getVisualizationSpecClass() {
    return TableSpec.class;
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
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVars(pluginSpec.getOutputVariable()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {

    // create scanner and line parser
    Scanner s = new Scanner(dataStreams.get(DEFAULT_SINGLE_STREAM_NAME)).useDelimiter(NL);
    DelimitedDataParser parser = new DelimitedDataParser(s.nextLine(), TAB, true);

    //print header
    List<String> header = parser.getColumnNames();
    out.write("{\"columns\":[".getBytes());
    boolean first = true;
    for (String colName : header) {
      if (first) first = false; else out.write(",".getBytes());
      System.err.println("col name: " + colName);
      String varSpec[] = colName.split("\\.");
      System.err.println(varSpec.toString());
      out.write(new JSONObject()
          .put("entityId", varSpec[0])
          .put("variableId", varSpec[1])
          .toString()
          .getBytes()
        );
    }
    out.write("],{\"rows\":[".getBytes());
    
    //loop through and print data
    first = true;
    while (s.hasNextLine()) {
      String[] row = parser.parseLineToArray(s.nextLine());
      if (first) first = false; else out.write(",".getBytes());
      out.write(new JSONArray(row).toString().getBytes());
    }
    
    // close array and enclosing object
    out.write("]}".getBytes());
    out.flush();
  }
}
