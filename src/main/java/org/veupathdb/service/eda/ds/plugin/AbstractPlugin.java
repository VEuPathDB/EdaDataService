package org.veupathdb.service.eda.ds.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementSet;
import org.veupathdb.service.eda.common.plugin.constraint.DataElementValidator;
import org.veupathdb.service.eda.common.client.NonEmptyResultStream;
import org.veupathdb.service.eda.generated.model.APIFilter;
import org.veupathdb.service.eda.generated.model.APIStudyDetail;
import org.veupathdb.service.eda.generated.model.BinSpec;
import org.veupathdb.service.eda.generated.model.BinSpec.RangeType;
import org.veupathdb.service.eda.generated.model.DateRange;
import org.veupathdb.service.eda.generated.model.DerivedVariable;
import org.veupathdb.service.eda.generated.model.GeolocationViewport;
import org.veupathdb.service.eda.generated.model.NumberRange;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.generated.model.VisualizationRequestBase;
import org.veupathdb.service.eda.generated.model.NumericViewport;

import static org.veupathdb.service.eda.common.plugin.util.PluginUtil.doubleQuote;
import static org.veupathdb.service.eda.common.plugin.util.PluginUtil.singleQuote;

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
  protected static final String DEFAULT_SINGLE_STREAM_NAME = "single_tabular_dataset";

  protected ReferenceMetadata _referenceMetadata;
  protected S _pluginSpec;
  protected List<StreamSpec> _requiredStreams;

  // methods that need to be implemented
  protected abstract Class<S> getVisualizationSpecClass();
  protected abstract void validateVisualizationSpec(S pluginSpec) throws ValidationException;
  protected abstract List<StreamSpec> getRequestedStreams(S pluginSpec);
  protected abstract void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException;

  // methods that should probably be overridden
  public String getDisplayName() { return getClass().getName(); }
  public String getDescription() { return ""; }
  public List<String> getProjects() { return Collections.emptyList(); }
  // have to decide if default is 1 and 25 override or vice versa. to facet or not, that is the question...
  public Integer getMaxPanels() { return 1; }
  public ConstraintSpec getConstraintSpec() { return new ConstraintSpec(); }

  // option for subclass to load any additional information (e.g. compute spec) from the request
  protected void loadAdditionalConfig(String appName, T request) throws ValidationException {}

  private Timer _timer;
  private boolean _requestProcessed = false;
  private List<APIFilter> _subset;
  private List<DerivedVariable> _derivedVariables;
  private EdaSubsettingClient _subsettingClient;
  private EdaMergingClient _mergingClient;

  public final AbstractPlugin<T,S> processRequest(String appName, T request, Entry<String,String> authHeader) throws ValidationException {

    // start request timer (used to profile request performance dynamics)
    _timer = new Timer();
    logRequestTime("Starting timer");

    // validate config type matches class provided by subclass
    _pluginSpec = getSpecObject(request, "getConfig", getVisualizationSpecClass());

    // check for subset and derived entity properties of request
    _subset = Optional.ofNullable(request.getFilters()).orElse(Collections.emptyList());
    _derivedVariables = Optional.ofNullable(request.getDerivedVariables()).orElse(Collections.emptyList());

    // build clients for required services
    _subsettingClient = new EdaSubsettingClient(Resources.SUBSETTING_SERVICE_URL, authHeader);
    _mergingClient = new EdaMergingClient(Resources.MERGING_SERVICE_URL, authHeader);

    // get study
    APIStudyDetail study = _subsettingClient.getStudy(request.getStudyId())
        .orElseThrow(() -> new ValidationException("Study '" + request.getStudyId() + "' does not exist."));

    // construct available variables for each entity from metadata and derived variable config
    _referenceMetadata = new ReferenceMetadata(study, _derivedVariables);

    // ask subclass to load any additional information (e.g. compute spec)
    loadAdditionalConfig(appName, request);

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

  protected PluginUtil getUtil() {
    return new PluginUtil(getReferenceMetadata(), _mergingClient);
  }
  protected void validateInputs(DataElementSet values) throws ValidationException {
    new DataElementValidator(getReferenceMetadata(), getConstraintSpec()).validate(values);
  }

  @SuppressWarnings("unchecked")
  protected <Q> Q getSpecObject(T request, String methodName, Class<Q> specClass) {
    try {
      Method configGetter = request.getClass().getMethod(methodName);
      Object config = configGetter.invoke(request);
      if (specClass.isAssignableFrom(config.getClass())) {
        return (Q)config;
      }
      throw new RuntimeException("Plugin class " + getClass().getName() +
          " declares spec class "  + specClass.getName() +
          " but " + request.getClass().getName() + "::" + methodName + "()" +
          " returned " + config.getClass().getName() + ". The second must be a subclass of the first.");
    }
    catch (NoSuchMethodException noSuchMethodException) {
      throw new RuntimeException("Generated class " + request.getClass().getName() +
          " must implement a no-arg method " + methodName + "() which returns an instance of " + specClass.getName());
    }
    catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("Misconfiguration of visualization plugin: " + getClass().getName(), e);
    }
  }

  /*****************************************************************
   *** Shared plugin-specific utilities
   ****************************************************************/

  protected void validateBinSpec(BinSpec binSpec, String xVarType) {
    if (xVarType.equals("NUMBER") || xVarType.equals("INTEGER")) {
      if (binSpec.getUnits() != null) {
        LOG.warn("The `units` property of the `BinSpec` class is only used for DATE x-axis variables. It will be ignored.");
      }
    }
    // need an error here if its a date and we dont have a unit?
  }

  public static String getBinRangeAsRString(RangeType binRange) {
    if (binRange != null) {
      if (binRange.isNumberRange()) {
        return(getBinRangeAsRString(binRange.getNumberRange()));
      } else {
        return(getBinRangeAsRString(binRange.getDateRange()));
      }
    } else {
      return("binRange <- NULL");
    }
  }

  public static String getBinRangeAsRString(NumberRange binRange) {
    if (binRange != null) {
      return("binRange <- list('min'=" + binRange.getMin().toString() + ", 'max'=" + binRange.getMax().toString() + ")");
    } else {
      return("binRange <- NULL");
    }
  }

  public static String getBinRangeAsRString(DateRange binRange) {
    if (binRange != null) {
      return("binRange <- list('min'='" + binRange.getMin() + "', 'max'='" + binRange.getMax() + "')");
    } else {
      return("binRange <- NULL");
    }
  }

  public static String getViewportAsRString(NumericViewport viewport, String xVarType) {
    if (viewport != null) {
      // think if we just pass the string plot.data will convert it to the claimed type
      if (xVarType.equals("NUMBER") || xVarType.equals("INTEGER")) {
        return("viewport <- list('xMin'=" + viewport.getXMin() + ", 'xMax'=" + viewport.getXMax() + ")");
      } else {
        return("viewport <- list('xMin'=" + singleQuote(viewport.getXMin()) + ", 'xMax'=" + singleQuote(viewport.getXMax()) + ")");
      }
    } else {
      return("viewport <- NULL");
    }
  }

  public static String getViewportAsRString(GeolocationViewport viewport) {
    if (viewport != null) {
      return("viewport <- list('latitude'=list('xMin'= " + viewport.getLatitude().getXMin() +
          ", 'xMax'= " + viewport.getLatitude().getXMax() +
          "), 'longitude'= list('left'= " + viewport.getLongitude().getLeft() +
          ", 'right' = " + viewport.getLongitude().getRight() + "))");
    } else {
      return("viewport <- NULL");
    }
  }

  // there is probably some JRI util that would make this unnecessary if i were more clever??
  public static String listToRVector(List<String> values) {
    boolean first = true;
    String vector = "c(";

    for (String value : values) {
      if (first) {
        vector = vector + doubleQuote(value);
        first = false;
      } else {
        vector = vector + ", " + doubleQuote(value);
      }
    }

    vector = vector + ")";
    return(vector);
  }

  // TODO need to get imputeZero and isCollection here as well
  // need to check for helpers in util/ EdaCommon classes
  public String getVariableMetadataRObjectAsString(VariableSpec var, String plotReference) {
    PluginUtil util = getUtil();
    
    return("new('VariableMetadata'," + 
                  "variableClass=new('VariableClass',value='native')," + 
                  "variableSpec=new('VariableSpec', variableId=" + singleQuote(var.getVariableId()) + ",entityId=" + singleQuote(var.getEntityId()) + ")," +
                  "plotReference=new('PlotReference', value=" + singleQuote(plotReference) + ")," +
                  "dataType=" + singleQuote(util.getVariableType(var)) + "," +
                  "dataShape=" + singleQuote(util.getVariableDataShape(var)) + ")");
  }

  // TODO make sure all R packages are passing tests after the update for plotRef
  // TODO need to do similar for computed var metadata and update compute plugins once i hear back from ryan
  public String getVoidEvalVariableMetadataList(Map<String, VariableSpec> vars) {
    boolean first = true;
    String variableMetadataList = new String("variables <- new('VariableMetadataList',");
    for(Map.Entry<String, VariableSpec> entry : vars.entrySet()) {
      VariableSpec var = entry.getValue();
      String variableMetadata = getVariableMetadataRObjectAsString(var);
      if (first) {
        first = false;
        variableMetadataList = variableMetadata;
      } else {
        variableMetadataList = variableMetadataList + "," + variableMetadata;
      }
    }
    variableMetadataList = variableMetadataList + ")";

    return(variableMetadataList);
  }

  // replacing this data.frame map w a dedicated VariableMetadataList S4 object
  public String getVoidEvalVarMetadataMap(String datasetName, Map<String, VariableSpec> vars) {
    boolean first = true;
    String plotRefVector = new String();
    String varColNameVector = new String();
    String varTypeVector = new String();
    String varShapeVector = new String();

    PluginUtil util = getUtil();
    for(Map.Entry<String, VariableSpec> entry : vars.entrySet()) {
      String plotRef = entry.getKey();
      VariableSpec var = entry.getValue();
      String varName = util.toColNameOrEmpty(var);
      if (varName.equals("")) continue;
      String varType = util.getVariableType(var);
      String varShape = util.getVariableDataShape(var);
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
