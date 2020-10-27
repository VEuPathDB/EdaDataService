package org.veupathdb.service.edads.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.gusdb.fgputil.AutoCloseableList;
import org.gusdb.fgputil.validation.ValidationBundle;
import org.gusdb.fgputil.validation.ValidationException;
import org.gusdb.fgputil.validation.ValidationLevel;
import org.veupathdb.service.edads.generated.model.APIEntity;
import org.veupathdb.service.edads.generated.model.APIFilter;
import org.veupathdb.service.edads.generated.model.APIStudyDetail;
import org.veupathdb.service.edads.generated.model.APIVariableType;
import org.veupathdb.service.edads.generated.model.BaseAnalysisConfig;
import org.veupathdb.service.edads.generated.model.DerivedVariable;
import org.veupathdb.service.edads.model.EntityDef;
import org.veupathdb.service.edads.model.StreamSpec;
import org.veupathdb.service.edads.model.VariableDef;
import org.veupathdb.service.edads.service.EdaClient;
import org.veupathdb.service.edads.service.StudiesService;

public abstract class AbstractEdadsPlugin<T extends BaseAnalysisConfig, S> implements Consumer<OutputStream> {

  protected abstract Class<S> getConfigurationClass();
  protected abstract ValidationBundle validate(S pluginSpec, Map<String,EntityDef> entities);
  protected abstract List<StreamSpec> getRequestedStreams(S pluginSpec, Map<String,EntityDef> supplementedEntities);
  protected abstract void writeResults(OutputStream out, List<InputStream> dataStreams) throws IOException;

  private boolean _requestProcessed = false;
  private APIStudyDetail _study;
  private Optional<List<APIFilter>> _subset;
  private Optional<List<DerivedVariable>> _derivedVariables;
  private List<StreamSpec> _requiredStreams;

  public final AbstractEdadsPlugin<T,S> processRequest(T request) throws ValidationException {

    // validate config type matches class provided by subclass
    S pluginSpec = getSpecObject(request);

    // validate requested study exists and fetch metadata
    _study = StudiesService.getStudy(request.getStudy());

    // check for subset and derived entity properties of request
    _subset = Optional.ofNullable(request.getSubset());
    _derivedVariables = Optional.ofNullable(request.getDerivedVariables());

    // construct available variables for each entity from metadata and derived variable config
    Map<String, EntityDef> supplementedEntities = supplementEntities(_study.getRootEntity(), _derivedVariables);

    // ask subclass to validate the configuration
    validate(pluginSpec, supplementedEntities).throwIfInvalid();

    // get list of data streams required by this subclass
    _requiredStreams = getRequestedStreams(pluginSpec, supplementedEntities);

    // validate stream specs provided by the subclass
    validate(_requiredStreams, supplementedEntities).throwIfInvalid();

    _requestProcessed = true;
    return this;
  }

  private ValidationBundle validate(List<StreamSpec> requestedStreams, Map<String,EntityDef> entities) {
    ValidationBundle.ValidationBundleBuilder validation = ValidationBundle.builder(ValidationLevel.RUNNABLE);
    for (StreamSpec spec : requestedStreams) {
      for (VariableDef requestedVar : spec) {
        if (!spec.getEntity().containsKey(requestedVar.getName())) {
          validation.addError("Entity " + spec.getEntity().getName() + " does not contain variable " + requestedVar.getName());
        }
      }
    }
    return validation.build();
  }

  // can erase tree structure here since we only care about variables available on a particular entity
  private static Map<String, EntityDef> supplementEntities(APIEntity entity, Optional<List<DerivedVariable>> derivedVariables) {
    Map<String, EntityDef> entities = new HashMap<>();
    EntityDef entityDef = new EntityDef(entity.getId(), entity.getName());

    // add variables for this entity
    entity.getVariables().stream()
      .filter(var -> !var.getType().equals(APIVariableType.CATEGORY))
      .map(var -> new VariableDef(var.getId(), var.getName(), var.getType()))
      .forEach(vd -> entityDef.put(vd.getName(), vd));

    // add derived variables for this entity
    derivedVariables
      .map(list -> list.stream()
        .filter(dr -> dr.getEntityId().equals(entity.getId()))
        .map(dr -> new VariableDef(dr.getName(), dr.getName(), dr.getType()))
        .filter(vd -> !entityDef.containsKey(vd.getName())) // skip if entity already contains the variable; will throw later
        .collect(Collectors.toList()))
      .orElse(Collections.emptyList())
      .forEach(vd -> entityDef.put(vd.getName(), vd));

    entities.put(entityDef.getName(), entityDef);
    entity.getChildren()
      .forEach(child -> entities.putAll(supplementEntities(child, derivedVariables)));
    return entities;
  }

  @SuppressWarnings("unchecked")
  private S getSpecObject(T request) {
    try {
      Method configGetter = request.getClass().getMethod("getConfig");
      Object config = configGetter.invoke(request);
      if (getConfigurationClass().equals(config.getClass())) {
        return (S)config;
      }
      throw new RuntimeException("Plugin class " + getClass().getName() +
          " does not declare the same spec class as that generated by its " +
          "endpoint's API.  The return type of getConfigurationClass() must be changed.");
    }
    catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("Misconfiguration of analysis plugin: " + getClass().getName(), e);
    }
  }

  @Override
  public void accept(OutputStream out) {
    try {
      if (!_requestProcessed) {
        throw new RuntimeException("Output cannot be streamed until request has been processed.");
      }
      try(AutoCloseableList<InputStream> dataStreams = buildDataStreams()) {
        writeResults(out, dataStreams);
      }
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to stream results", e);
    }
  }

  private AutoCloseableList<InputStream> buildDataStreams() {
    AutoCloseableList<InputStream> dataStreams = new AutoCloseableList<>();
    try {
      for (StreamSpec spec : _requiredStreams) {
        dataStreams.add(EdaClient.getDataStream(
            _study,
            _subset,
            _derivedVariables,
            spec
        ));
      }
      return dataStreams;
    }
    catch (Exception e) {
      dataStreams.close();
      throw new RuntimeException("Unable to fetch all required data", e);
    }
  }

}
