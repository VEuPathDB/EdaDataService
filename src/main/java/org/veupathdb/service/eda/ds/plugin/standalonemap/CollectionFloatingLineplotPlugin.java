package org.veupathdb.service.eda.ds.plugin.standalonemap;

import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.core.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.ds.utils.ValidationUtils;
import org.veupathdb.service.eda.generated.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.veupathdb.service.eda.common.plugin.util.PluginUtil.singleQuote;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.streamResult;
import static org.veupathdb.service.eda.common.plugin.util.RServeClient.useRConnectionWithRemoteFiles;
import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class CollectionFloatingLineplotPlugin extends AbstractEmptyComputePlugin<CollectionFloatingLineplotPostRequest, CollectionFloatingLineplotSpec> {

  @Override
  public String getDisplayName() {
    return "Line plot";
  }

  @Override
  public String getDescription() {
    return "Visualize aggregate values of one variable across the sequential values of an ordered Variable Group";
  }

  @Override
  public List<String> getProjects() {
    return List.of(VECTORBASE_PROJECT);
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    return new ConstraintSpec()
      .dependencyOrder(List.of("yAxisVariable"), List.of("xAxisVariable", "overlayVariable"))
      .pattern()
        .element("xAxisVariable")
          .shapes(APIVariableDataShape.ORDINAL, APIVariableDataShape.CONTINUOUS)
          .description("Variable must be ordinal, a number, or a date and be the same or a child entity as the variable the map markers are painted with.")
      .done();
  }

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new EmptyComputeClassGroup(CollectionFloatingLineplotPostRequest.class, CollectionFloatingLineplotSpec.class);
  }

  @Override
  protected void validateVisualizationSpec(CollectionFloatingLineplotSpec pluginSpec) throws ValidationException {
    validateInputs(new DataElementSet()
      .entity(pluginSpec.getOutputEntityId())
      .var("xAxisVariable", pluginSpec.getXAxisVariable()));
    ValidationUtils.validateCollectionMembers(getUtil(),
        pluginSpec.getOverlayConfig().getCollection(),
        pluginSpec.getOverlayConfig().getSelectedMembers());
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(CollectionFloatingLineplotSpec pluginSpec) {
    String outputEntityId = pluginSpec.getOutputEntityId();
    List<VariableSpec> plotVariableSpecs = new ArrayList<VariableSpec>();
    plotVariableSpecs.add(pluginSpec.getXAxisVariable());
    plotVariableSpecs.addAll(pluginSpec.getOverlayConfig().getSelectedMembers());

    return ListBuilder.asList(
      new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, outputEntityId)
        .addVars(plotVariableSpecs)
        // TODO can we make this automagical?
        .addVars(getVariableSpecsWithStudyDependentVocabs(pluginSpec.getOutputEntityId(), plotVariableSpecs)));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    PluginUtil util = getUtil();
    CollectionFloatingLineplotSpec spec = getPluginSpec();
    List<VariableSpec> inputVarSpecs = new ArrayList<>(spec.getOverlayConfig().getSelectedMembers());
    inputVarSpecs.add(spec.getXAxisVariable());
    CollectionSpec overlayVariable = spec.getOverlayConfig().getCollection();
    Map<String, DynamicDataSpec> varMap = new HashMap<>();
    varMap.put("xAxis", new DynamicDataSpecImpl(spec.getXAxisVariable()));
    varMap.put("overlay", new DynamicDataSpecImpl(overlayVariable));
    String errorBars = spec.getErrorBars() != null ? spec.getErrorBars().getValue() : "FALSE";
    String valueSpec = spec.getValueSpec().getValue();
    String collectionType = util.getCollectionType(overlayVariable);
    String numeratorValues = spec.getYAxisNumeratorValues() != null ? PluginUtil.listToRVector(spec.getYAxisNumeratorValues()) : "NULL";
    String denominatorValues = spec.getYAxisDenominatorValues() != null ? PluginUtil.listToRVector(spec.getYAxisDenominatorValues()) : "NULL";
    String overlayValues = getRBinListAsString(spec.getOverlayConfig().getSelectedValues());
   
    List<DynamicDataSpec> dataSpecsWithStudyDependentVocabs = findDataSpecsWithStudyDependentVocabs(varMap);
    Map<String, InputStream> studyVocabs = getVocabByRootEntity(dataSpecsWithStudyDependentVocabs);
    dataStreams.putAll(studyVocabs);
     
    useRConnectionWithRemoteFiles(Resources.RSERVE_URL, dataStreams, connection -> {
      connection.voidEval(DEFAULT_SINGLE_STREAM_NAME + " <- data.table::fread('" + DEFAULT_SINGLE_STREAM_NAME + "', na.strings=c(''))");
      String inputData = getRInputDataWithImputedZeroesAsString(DEFAULT_SINGLE_STREAM_NAME, varMap, "variables");
      connection.voidEval(getVoidEvalDynamicDataMetadataList(varMap));
      String viewportRString = getViewportAsRString(spec.getViewport(), collectionType);
      connection.voidEval(viewportRString);
      BinWidthSpec binSpec = spec.getBinSpec();
      validateBinSpec(binSpec, collectionType);
     
      String binWidth;
      if (collectionType.equals("NUMBER") || collectionType.equals("INTEGER")) {
        binWidth = binSpec.getValue() == null ? "NULL" : "as.numeric('" + binSpec.getValue() + "')";
      } else {
        binWidth = binSpec.getValue() == null ? "NULL" : "'" + binSpec.getValue().toString() + " " + binSpec.getUnits().toString().toLowerCase() + "'";
      }
      connection.voidEval("binWidth <- " + binWidth);

      String cmd = "plot.data::lineplot(data=" + inputData + ", " +
                                        "variables=variables, binWidth=binWidth, " + 
                                        "value=" + singleQuote(valueSpec) + ", " +
                                        "errorBars=" + errorBars + ", " +
                                        "viewport=viewport, " +
                                        "numeratorValues=" + numeratorValues + ", " +
                                        "denominatorValues=" + denominatorValues + ", " +
                                        "sampleSizes=FALSE," +
                                        "completeCases=FALSE," +
                                        "overlayValues=" + overlayValues + ", " +
                                        "evilMode='noVariables')";                          
      streamResult(connection, cmd, out);
    }); 
  }
}
