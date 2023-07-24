package org.veupathdb.service.eda.ds.plugin.standalonemap;

import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.json.JSONObject;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.OverlaySpecification;
import org.veupathdb.service.eda.ds.utils.ValidationUtils;
import org.veupathdb.service.eda.generated.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class CollectionFloatingBarplotPlugin extends AbstractEmptyComputePlugin<CollectionFloatingBarplotPostRequest, CollectionFloatingBarplotSpec> {

  @Override
  public String getDisplayName() {
    return "Bar plot";
  }

  @Override
  public String getDescription() {
    return "Visualize the distribution of a group of categorical variables";
  }

  @Override
  public List<String> getProjects() {
    return List.of(VECTORBASE_PROJECT);
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("xAxisVariable"), List.of("overlayVariable"))
      .pattern()
        .element("xAxisVariable")
          .maxValues(10)
          .description("Variable Group vocabulary must have 10 or fewer unique values.")
        .element("overlayVariable")
      .done();
  }

  @Override
  protected AbstractPlugin<CollectionFloatingBarplotPostRequest, CollectionFloatingBarplotSpec, Void>.ClassGroup getTypeParameterClasses() {
    return new ClassGroup(CollectionFloatingBarplotPostRequest.class, CollectionFloatingBarplotSpec.class, Void.class);
  }

  @Override
  protected void validateVisualizationSpec(CollectionFloatingBarplotSpec pluginSpec) throws ValidationException {
    ValidationUtils.validateCollectionMembers(getUtil(),
        pluginSpec.getOverlayConfig().getCollection(),
        pluginSpec.getOverlayConfig().getSelectedMembers());
    if (pluginSpec.getBarMode() == null) {
      throw new ValidationException("Property 'barMode' is required.");
    }
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(CollectionFloatingBarplotSpec pluginSpec) {    
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVars(pluginSpec.getOverlayConfig().getSelectedMembers())
      ); 
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    CollectionFloatingBarplotSpec spec = getPluginSpec();
    PluginUtil util = getUtil();
    String barMode = spec.getBarMode().getValue();
    String overlayValues = getRBinListAsString(spec.getOverlayConfig().getSelectedValues());
    List<VariableSpec> inputVarSpecs = new ArrayList<>(spec.getOverlayConfig().getSelectedMembers());

    Map<String, CollectionSpec> varMap = new HashMap<String, CollectionSpec>();
    varMap.put("overlay", spec.getOverlayConfig().getCollection());
      
    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME, inputVarSpecs));
      connection.voidEval(getVoidEvalVariableMetadataList(varMap));
      String cmd =
          "plot.data::bar(data=" + DEFAULT_SINGLE_STREAM_NAME + ", " +
              "variables=variables, " +
              "value='" + spec.getValueSpec().getValue() + "', " +
              "barmode='" + barMode + "', " +
              "sampleSizes=FALSE, " +
              "completeCases=FALSE, " + 
              "overlayValues=" + overlayValues + ", " + 
              "evilMode='noVariables')";
      streamResult(connection, cmd, out);
    });
  }
}
