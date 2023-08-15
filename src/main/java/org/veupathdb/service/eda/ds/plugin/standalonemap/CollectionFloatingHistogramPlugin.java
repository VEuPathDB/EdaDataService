package org.veupathdb.service.eda.ds.plugin.standalonemap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
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
import java.util.stream.Collectors;

import static org.veupathdb.service.eda.common.plugin.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class CollectionFloatingHistogramPlugin extends AbstractEmptyComputePlugin<CollectionFloatingHistogramPostRequest, CollectionFloatingHistogramSpec> {
  private OverlaySpecification _overlaySpecification = null;
  
  private static final Logger LOG = LogManager.getLogger(FloatingHistogramPlugin.class);

  @Override
  public String getDisplayName() {
    return "Histogram";
  }

  @Override
  public String getDescription() {
    return "Visualize the distribution of a continuous Variable Group";
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
          .types(APIVariableType.NUMBER, APIVariableType.DATE, APIVariableType.INTEGER)
          .description("Variable must be a number or date.")
        .element("overlayVariable")
      .done();
  }

  @Override
  protected AbstractPlugin<CollectionFloatingHistogramPostRequest, CollectionFloatingHistogramSpec, Void>.ClassGroup getTypeParameterClasses() {
    return new ClassGroup(CollectionFloatingHistogramPostRequest.class, CollectionFloatingHistogramSpec.class, Void.class);
  }

  @Override
  protected void validateVisualizationSpec(CollectionFloatingHistogramSpec pluginSpec) throws ValidationException {
    ValidationUtils.validateCollectionMembers(getUtil(),
        pluginSpec.getOverlayConfig().getCollection(),
        pluginSpec.getOverlayConfig().getSelectedMembers());
    if (pluginSpec.getBarMode() == null) {
      throw new ValidationException("Property 'barMode' is required.");
    }
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(CollectionFloatingHistogramSpec pluginSpec) {
    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId())
        .addVars(pluginSpec.getOverlayConfig().getSelectedMembers())
      );
  }
  
  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    PluginUtil util = getUtil();
    CollectionFloatingHistogramSpec spec = getPluginSpec();
    List<VariableSpec> inputVarSpecs = new ArrayList<>(spec.getOverlayConfig().getSelectedMembers());
    CollectionSpec overlayVariable = spec.getOverlayConfig().getCollection();
    Map<String, CollectionSpec> varMap = new HashMap<String, CollectionSpec>();
    varMap.put("overlay", overlayVariable);
    String barMode = spec.getBarMode().getValue();
    String collectionType = util.getCollectionType(overlayVariable);

    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(util.getVoidEvalFreadCommand(DEFAULT_SINGLE_STREAM_NAME, inputVarSpecs));
      connection.voidEval(getVoidEvalCollectionMetadataList(varMap));
     
      String viewportRString = getViewportAsRString(spec.getViewport(), collectionType);
      connection.voidEval(viewportRString);
      
      BinWidthSpec binSpec = spec.getBinSpec();
      validateBinSpec(binSpec, collectionType);
      String binReportValue = binSpec.getType().getValue() != null ? binSpec.getType().getValue() : "binWidth";
      
      String binWidth = "NULL";
      if (collectionType.equals("NUMBER") || collectionType.equals("INTEGER")) {
        binWidth = binSpec.getValue() == null ? "NULL" : "as.numeric('" + binSpec.getValue() + "')";
      } else {
        binWidth = binSpec.getValue() == null ? "NULL" : "'" + binSpec.getValue().toString() + " " + binSpec.getUnits().toString().toLowerCase() + "'";
      }
      connection.voidEval("binWidth <- " + binWidth);

      String cmd =
          "plot.data::histogram(data=" + DEFAULT_SINGLE_STREAM_NAME + ", " +
                                  "variables=variables, " +
                                  "binWidth=binWidth, " +
                                  "value='" + spec.getValueSpec().getValue() + "', " +
                                  "binReportValue='" + binReportValue + "', " +
                                  "barmode='" + barMode + "', " +
                                  "viewport=viewport, " + 
                                  "sampleSizes=FALSE, " +
                                  "completeCases=FALSE, " +
                                  "overlayValues=NULL, " +
                                  "evilMode='noVariables')";
      streamResult(connection, cmd, out);
    });
  }
}
