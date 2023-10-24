package org.veupathdb.service.eda.compute.plugins.example;

import org.gusdb.fgputil.json.JsonUtil;
import org.jetbrains.annotations.NotNull;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.compute.plugins.AbstractPlugin;
import org.veupathdb.service.eda.compute.plugins.PluginContext;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.ComputedVariableMetadata;
import org.veupathdb.service.eda.generated.model.ComputedVariableMetadataImpl;
import org.veupathdb.service.eda.generated.model.ExampleComputeConfig;
import org.veupathdb.service.eda.generated.model.ExamplePluginRequest;
import org.veupathdb.service.eda.generated.model.ExamplePluginStats;
import org.veupathdb.service.eda.generated.model.ExamplePluginStatsImpl;
import org.veupathdb.service.eda.generated.model.PlotReferenceValue;
import org.veupathdb.service.eda.generated.model.VariableClass;
import org.veupathdb.service.eda.generated.model.VariableMapping;
import org.veupathdb.service.eda.generated.model.VariableMappingImpl;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ExamplePlugin extends AbstractPlugin<ExamplePluginRequest, ExampleComputeConfig> {

  private static final String INPUT_DATA = "example-input";
  private static final String COMPUTED_COLUMN_NAME_SUFFIX = "WithSuffix";

  public ExamplePlugin(@NotNull PluginContext<ExamplePluginRequest, ExampleComputeConfig> context) {
    super(context);
  }

  @NotNull
  @Override
  public List<StreamSpec> getStreamSpecs() {
    ExampleComputeConfig config = getConfig();
    return List.of(new StreamSpec(INPUT_DATA, config.getInputVariable().getEntityId())
        .addVars(List.of(config.getInputVariable())));
  }

  @Override
  protected void execute() {
    AtomicInteger numEmptyValues = new AtomicInteger(0); // just using mutability here

    // write the tabular result
    getWorkspace().writeDataResult(outStream -> {
      try (BufferedReader in = new BufferedReader(new InputStreamReader(getWorkspace().openStream(INPUT_DATA)))) {
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outStream));
        out.write(convertMergeHeader(in.readLine()));
        out.newLine();
        while (in.ready()) {
          String line = in.readLine();
          if (line.endsWith("\t")) {
            // a tab at the end means no value for this var on this record; value with suffix should also be empty
            numEmptyValues.incrementAndGet();
          } else {
            // appending the suffix to the line is akin to appending to the single var value
            line += getConfig().getValueSuffix();
          }
          out.write(line);
          out.newLine();
        }
        out.flush();
      } catch (IOException e) {
        throw new RuntimeException("Unable to convert incoming tabular stream or write to output", e);
      }
    });

    // write the metadata result
    String computedVariableName = getConfig().getInputVariable().getVariableId() + COMPUTED_COLUMN_NAME_SUFFIX;
    VariableSpec computedVariableSpec = VariableDef.newVariableSpec(getConfig().getInputVariable().getEntityId(), computedVariableName);
    getWorkspace().writeMetaResult(createMetadataObject(computedVariableSpec));

    // write the statistics result
    ExamplePluginStats stats = new ExamplePluginStatsImpl();
    stats.setNumEmptyValues(numEmptyValues.get());
    getWorkspace().writeStatisticsResult(JsonUtil.serializeObject(stats));
  }

  private String convertMergeHeader(String mergeServiceHeader) {
    // merge service produces column header names in 'dot' format since its columns may come from various entities;
    // for this simple plugin, want to trim off the entity IDs since we're just echoing the cols.
    return Arrays.stream(mergeServiceHeader.split("\t"))
        .map(column -> column.split("\\.")[1])
        .collect(Collectors.joining("\t"))
        // additional trick: adding suffix to whole row will make new name compliant with dot notation, matching declared name above
        + COMPUTED_COLUMN_NAME_SUFFIX;
  }

  private static ComputedVariableMetadata createMetadataObject(VariableSpec computedVariableSpec) {

    // build the metadata for the variable
    VariableMapping var = new VariableMappingImpl();
    var.setVariableSpec(computedVariableSpec);
    var.setDataType(APIVariableType.STRING);
    var.setDataShape(APIVariableDataShape.CONTINUOUS);
    var.setVariableClass(VariableClass.COMPUTED);
    var.setDisplayName("Example Computed Variable");
    var.setImputeZero(false);
    var.setIsCollection(false);
    var.setPlotReference(PlotReferenceValue.XAXIS);

    ComputedVariableMetadata meta = new ComputedVariableMetadataImpl();
    meta.setVariables(List.of(var));
    return meta;
  }
}
