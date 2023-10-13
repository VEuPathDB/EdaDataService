package org.veupathdb.service.eda.subset.service;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.veupathdb.service.eda.generated.model.APIFilter;
import org.veupathdb.service.eda.generated.model.APITabularReportConfig;
import org.veupathdb.service.eda.generated.model.SortSpecEntry;
import org.veupathdb.service.eda.ss.model.Entity;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.filter.Filter;
import org.veupathdb.service.eda.ss.model.tabular.TabularReportConfig;
import org.veupathdb.service.eda.ss.model.variable.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RequestBundle {

  private static final Logger LOG = LogManager.getLogger(RequestBundle.class);

  static RequestBundle unpack(String dataSchema, Study study, String entityId, List<APIFilter> apiFilters, List<String> variableIds, APITabularReportConfig apiReportConfig) {

    Entity entity = study.getEntity(entityId).orElseThrow(() -> new NotFoundException("In " + study.getStudyId() + " Entity ID not found: " + entityId));

    List<VariableWithValues> variables = getEntityVariables(entity, variableIds);

    List<Filter> filters = ApiConversionUtil.toInternalFilters(study, apiFilters, dataSchema);

    TabularReportConfig reportConfig = getTabularReportConfig(entity, Optional.ofNullable(apiReportConfig));

    return new RequestBundle(study, entity, variables, filters, reportConfig);
  }

  private static TabularReportConfig getTabularReportConfig(Entity entity, Optional<APITabularReportConfig> configOpt) {
    TabularReportConfig config = new TabularReportConfig();

    if (configOpt.isEmpty()) {
      // use defaults for config
      return config;
    }

    APITabularReportConfig apiConfig = configOpt.get();

    // assign submitted paging if present
    if (apiConfig.getPaging() != null) {
      LOG.info("Num rows type: {}", apiConfig.getPaging().getNumRows().getClass());
      Long numRows = apiConfig.getPaging().getNumRows();
      if (numRows != null) {
        if (numRows <= 0)
          throw new BadRequestException("In paging config, numRows must a positive integer.");
        config.setNumRows(Optional.of(numRows));
      }
      Long offset = apiConfig.getPaging().getOffset();
      if (offset != null) {
        if (offset < 0)
          throw new BadRequestException("In paging config, offset must a non-negative integer.");
        config.setOffset(offset);
      }
    }

    // assign submitted sorting if present
    List<SortSpecEntry> sorting = apiConfig.getSorting();
    if (sorting != null && !sorting.isEmpty()) {
      for (SortSpecEntry entry : sorting) {
        entity.getVariableOrThrow(entry.getKey());
      }
      config.setSorting(ApiConversionUtil.toInternalSorting(sorting));
    }

    // assign header format if present
    if (apiConfig.getHeaderFormat() != null) {
      config.setHeaderFormat(ApiConversionUtil.toInternalTabularHeaderFormat(apiConfig.getHeaderFormat()));
    }

    // assign date trimming flag if present
    if (apiConfig.getTrimTimeFromDateVars() != null) {
      config.setTrimTimeFromDateVars(apiConfig.getTrimTimeFromDateVars());
    }

    if (apiConfig.getDataSource() != null) {
      config.setDataSourceType(ApiConversionUtil.toInternalDataSourceType(apiConfig.getDataSource()));
    }
    return config;
  }

  private static List<VariableWithValues> getEntityVariables(Entity entity, List<String> variableIds) {

    List<Variable> variables = new ArrayList<>();

    for (String varId : variableIds) {
      String errMsg = "Variable '" + varId + "' is not found for entity with ID: '" + entity.getId() + "'";
      variables.add(entity.getVariable(varId).orElseThrow(() -> new BadRequestException(errMsg)));
    }

    for (Variable var: variables) {
      if (!var.hasValues()) {
        throw new BadRequestException("Variable " + var.getId() + " is not a variable with values.");
      }
    }

    return variables.stream()
        .map(var -> (VariableWithValues) var)
        .collect(Collectors.toList());
  }

  private final Study _study;
  private final List<Filter> _filters;
  private final Entity _targetEntity;
  private final List<VariableWithValues> _requestedVariables;
  private final TabularReportConfig _reportConfig;

  RequestBundle(Study study, Entity targetEntity, List<VariableWithValues> requestedVariables, List<Filter> filters, TabularReportConfig reportConfig) {
    _study = study;
    _targetEntity = targetEntity;
    _filters = filters;
    _requestedVariables = requestedVariables;
    _reportConfig = reportConfig;
  }

  public Study getStudy() {
    return _study;
  }

  public List<Filter> getFilters() {
    return _filters;
  }

  public Entity getTargetEntity() {
    return _targetEntity;
  }

  public List<VariableWithValues> getRequestedVariables() {
    return _requestedVariables;
  }

  public TabularReportConfig getReportConfig() {
    return _reportConfig;
  }
}
