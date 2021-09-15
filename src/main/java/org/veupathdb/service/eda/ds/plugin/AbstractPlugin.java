package org.veupathdb.service.eda.ds.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.Timer;
import org.gusdb.fgputil.client.ResponseFuture;
import org.gusdb.fgputil.functional.FunctionalInterfaces.ConsumerWithException;
import org.gusdb.fgputil.functional.Functions;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.EdaMergingClient;
import org.veupathdb.service.eda.common.client.EdaSubsettingClient;
import org.veupathdb.service.eda.common.client.StreamingDataClient;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.constraints.DataElementSet;
import org.veupathdb.service.eda.ds.constraints.DataElementValidator;
import org.veupathdb.service.eda.ds.util.NonEmptyResultStream;
import org.veupathdb.service.eda.generated.model.APIFilter;
import org.veupathdb.service.eda.generated.model.APIStudyDetail;
import org.veupathdb.service.eda.generated.model.DerivedVariable;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.generated.model.VisualizationRequestBase;

/**
 * Base vizualization plugin for all other plugins.  Provides access to parts of
 * the request object, manages logic flow over the course of the request, and
 * provides streaming merged data to subclasses for processing per specs provided
 * by those subclasses.
 *
 * @param <T> type of request (must extend VisualizationRequestBase)
 * @param <S> plugin's spec class (must be or extend the generated spec class for this plugin)
 */
public abstract class AbstractPlugin<T extends VisualizationRequestBase, S> implements Consumer<OutputStream> {

  private static final Logger LOG = LogManager.getLogger(AbstractPlugin.class);

  // shared stream name for plugins that need request only a single stream
  protected static final String DEFAULT_SINGLE_STREAM_NAME = "single-tabular-dataset.txt";

  // methods that need to be implemented
  protected abstract Class<S> getVisualizationSpecClass();
  protected abstract void validateVisualizationSpec(S pluginSpec) throws ValidationException;
  protected abstract List<StreamSpec> getRequestedStreams(S pluginSpec);
  protected abstract void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException;

  // methods that should probably be overridden
  public String getDisplayName() { return getClass().getName(); }
  public String getDescription() { return ""; }
  public List<String> getProjects() { return Arrays.asList(""); }
  // have to decide if default is 1 and 25 override or vice versa. to facet or not, that is the question...
  public Integer getMaxPanels() { return 1; }
  public ConstraintSpec getConstraintSpec() { return new ConstraintSpec(); }

  private final EdaSubsettingClient _subsettingClient = new EdaSubsettingClient(Resources.SUBSETTING_SERVICE_URL);
  private final StreamingDataClient _mergingClient = new EdaMergingClient(Resources.MERGING_SERVICE_URL);

  private Timer _timer;
  private boolean _requestProcessed = false;
  private S _pluginSpec;
  private List<APIFilter> _subset;
  private List<DerivedVariable> _derivedVariables;
  private ReferenceMetadata _referenceMetadata;

  private List<StreamSpec> _requiredStreams;

  public final AbstractPlugin<T,S> processRequest(T request) throws ValidationException {

    // start request timer (used to profile request performance dynamics)
    _timer = new Timer();
    logRequestTime("Starting timer");

    // validate config type matches class provided by subclass
    _pluginSpec = getSpecObject(request);

    // check for subset and derived entity properties of request
    _subset = Optional.ofNullable(request.getFilters()).orElse(Collections.emptyList());
    _derivedVariables = Optional.ofNullable(request.getDerivedVariables()).orElse(Collections.emptyList());

    // get study
    APIStudyDetail study = _subsettingClient.getStudy(request.getStudyId())
        .orElseThrow(() -> new ValidationException("Study '" + request.getStudyId() + "' does not exist."));

    // construct available variables for each entity from metadata and derived variable config
    _referenceMetadata = new ReferenceMetadata(study, _derivedVariables);

    // ask subclass to validate the configuration
    validateVisualizationSpec(_pluginSpec);

    // get list of data streams required by this subclass
    _requiredStreams = getRequestedStreams(_pluginSpec);

    // validate stream specs provided by the subclass
    _mergingClient.getStreamSpecValidator()
        .validateStreamSpecs(_requiredStreams, _referenceMetadata).throwIfInvalid();

    _requestProcessed = true;
    logRequestTime("Initial request processing complete");
    return this;
  }

  protected void logRequestTime(String eventDescription) {
    LOG.info("Request Time: " + _timer.getElapsed() + "ms, " + eventDescription);
  }

  @Override
  public final void accept(OutputStream out) {
    if (!_requestProcessed) {
      throw new RuntimeException("Output cannot be streamed until request has been processed.");
    }

    // create stream generator
    Function<StreamSpec, ResponseFuture> streamGenerator = spec -> _mergingClient
        .getTabularDataStream(_referenceMetadata, _subset, spec);

    // create stream processor
    // TODO: might make disallowing empty results optional in the future; this is the original implementation
    //ConsumerWithException<Map<String,InputStream>> streamProcessor = map -> writeResults(out, map);
    ConsumerWithException<Map<String,InputStream>> streamProcessor = map -> writeResults(out,
        Functions.mapValues(map, entry -> new NonEmptyResultStream(entry.getKey(), entry.getValue())));

    // build and process streams
    logRequestTime("Making requests for data streams");
    LOG.info("Building and processing " + _requiredStreams.size() + " required data streams.");
    StreamingDataClient.buildAndProcessStreams(_requiredStreams, streamGenerator, streamProcessor);
    logRequestTime("Data streams processed; response written; request complete");
  }

  protected S getPluginSpec() {
    return _pluginSpec;
  }

  protected ReferenceMetadata getReferenceMetadata() {
    return _referenceMetadata;
  }

  protected void validateInputs(DataElementSet values) throws ValidationException {
    new DataElementValidator(getReferenceMetadata(), getConstraintSpec()).validate(values);
  }

  @SuppressWarnings("unchecked")
  private S getSpecObject(T request) {
    try {
      Method configGetter = request.getClass().getMethod("getConfig");
      Object config = configGetter.invoke(request);
      if (getVisualizationSpecClass().isAssignableFrom(config.getClass())) {
        return (S)config;
      }
      throw new RuntimeException("Plugin class " + getClass().getName() +
          " declares spec class "  + getVisualizationSpecClass().getName() +
          " but " + request.getClass().getName() + "::getConfig()" +
          " returned " + config.getClass().getName() + ". The second must be a subclass of the first.");
    }
    catch (NoSuchMethodException noSuchMethodException) {
      throw new RuntimeException("Generated class " + request.getClass().getName() +
          " must implement a no-arg method getConfig() which returns an instance of " + getVisualizationSpecClass().getName());
    }
    catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("Misconfiguration of visualization plugin: " + getClass().getName(), e);
    }
  }

  protected String toColNameOrEmpty(VariableSpec var) {
    return var == null ? "" : _mergingClient.varToColumnHeader(var);
  }

  protected String toColNameOrEmpty(List<VariableSpec> vars, int index) {
    VariableSpec var = getVariableSpecFromList(vars, index);
    String colName = toColNameOrEmpty(var);

    return colName;
  }

  /*****************************************************************
   *** Convenience utilities for subclasses
   ****************************************************************/

  protected VariableSpec getVariableSpecFromList(List<VariableSpec> vars, int index) {
    return vars == null || vars.size() <= index ? null : vars.get(index);
  }

  protected String getVariableEntityId(VariableSpec var) {
    return var == null ? null : var.getEntityId();
  }

  protected String getVariableEntityId(List<VariableSpec> vars, int index) {
    return getVariableEntityId(getVariableSpecFromList(vars, index));
  }

  private String getVariableAttribute(Function<VariableDef, ?> getter, VariableSpec var) {
    return var == null ? "" : getter.apply(getReferenceMetadata().getVariable(var).orElseThrow()).toString();
  }

  protected String getVariableType(VariableSpec var) {
    return getVariableAttribute(VariableDef::getType, var);
  }

  protected String getVariableType(List<VariableSpec> vars, int index) {
    return getVariableType(getVariableSpecFromList(vars, index));
  }

  protected String getVariableDataShape(VariableSpec var) {
    return getVariableAttribute(VariableDef::getDataShape, var);
  }

  protected String getVariableDataShape(List<VariableSpec> vars, int index) {
    return getVariableDataShape(getVariableSpecFromList(vars, index));
  }

  // Suggested helper to take array of var names, entities, types, or shapes, and rewrite them into one comma separated string.
  //  public static String toCommaSepString(String[] stringArray) {
  //    String commaString = "";
  //    for (int i = 0; i < stringArray.length; i++) {
  //      //Do your stuff here
  //      if (i < stringArray.length - 1){
  //        commaString += ("'" + stringArray[i] + "', ");
  //      } else {
  //        commaString += ("'" + stringArray[i] + "'");
  //      }
  //    }
  //    return commaString;
  //  }

  
  protected String singleQuote(String unquotedString) {
    return "'" + unquotedString + "'";
  }
  
  protected String getVoidEvalFreadCommand(String fileName, VariableSpec... vars) {    
    boolean first = true;
    String namedTypes = new String();
    
    for(VariableSpec var : vars) {
      String varName = toColNameOrEmpty(var);
      if (varName.equals("")) continue;
      String varType = getVariableType(var);
      String varShape = getVariableDataShape(var);
      if (varType.equals("NUMBER") & !varShape.equals("CATEGORICAL")) {
        varType = "double";
      } else {
        varType = "character";
      }
      if (first) {
        first = false;
        namedTypes = singleQuote(varName) + "=" + singleQuote(varType);
      } else {
        namedTypes = namedTypes + "," + singleQuote(varName) + "=" + singleQuote(varType);
      }
    }
        
    String freadCommand = "data <- fread(" + singleQuote(fileName) + 
                                         ", select=c(" + namedTypes + ")" +
                                         ", na.strings=c(''))";
    
    return freadCommand;
  }
  
  protected String getVoidEvalVarMetadataMap(Map<String, VariableSpec> vars) {
    boolean first = true;
    String plotRefVector = new String();
    String varColNameVector = new String();
    String varTypeVector = new String();
    String varShapeVector = new String();
    
    for(Map.Entry<String, VariableSpec> entry : vars.entrySet()) {
      String plotRef = entry.getKey();
      VariableSpec var = entry.getValue();
      String varName = toColNameOrEmpty(var);
      if (varName.equals("")) continue;
      String varType = getVariableType(var);
      String varShape = getVariableDataShape(var);
      if (first) {
        first = false;
        plotRefVector = singleQuote(plotRef);
        varColNameVector = singleQuote(varName);
        varTypeVector = singleQuote(varType);
        varShapeVector = singleQuote(varShape);
      } else {
        plotRefVector = plotRefVector + "," + singleQuote(plotRef);
        varColNameVector = varColNameVector + "," + singleQuote(varName);
        varTypeVector = varTypeVector + "," + singleQuote(varType);
        varShapeVector = varShapeVector + "," + singleQuote(varShape);
      }
    }
        
    String varMetadataMapString = "map <- data.frame("
        + "'plotRef'=c(" + plotRefVector + "), "
        + "'id'=c(" + varColNameVector + "), "
        + "'dataType'=c("+ varTypeVector + "), "
        + "'dataShape'=c(" + varShapeVector + "), stringsAsFactors=FALSE)";
    
    return varMetadataMapString;
  }
}
