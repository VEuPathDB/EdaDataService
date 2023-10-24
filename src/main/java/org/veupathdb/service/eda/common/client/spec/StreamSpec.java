package org.veupathdb.service.eda.common.client.spec;

import org.gusdb.fgputil.json.JsonUtil;
import org.json.JSONObject;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.generated.model.APIFilter;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.gusdb.fgputil.FormatUtil.NL;

/**
 * Specifies an entity and set of variables desired in a named tabular stream.
 *
 * Semantics differ slightly depending on whether this spec is being
 * submitted to the subsetting service vs the merging service.  For
 * example, if submitting to the subsetting service, all variables must
 * belong to the specified entity; merge service specs are more flexible
 * but also impose constraints.
 */
public class StreamSpec extends ArrayList<VariableSpec> {

  private final String _streamName;
  private final String _entityId;
  private boolean _includeComputedVars = false;
  private List<APIFilter> _overriddenFilters;

  public StreamSpec(String streamName, String entityId) {
    _streamName = streamName;
    _entityId = entityId;
  }

  public String getStreamName() {
    return _streamName;
  }

  public String getEntityId() {
    return _entityId;
  }

  public boolean isIncludeComputedVars() {
    return _includeComputedVars;
  }

  public StreamSpec setIncludeComputedVars(boolean includeComputedVars) {
    _includeComputedVars = includeComputedVars;
    return this;
  }

  public Optional<List<APIFilter>> getFiltersOverride() {
    return Optional.ofNullable(_overriddenFilters);
  }

  /**
   * Override the default filters (filter set sent with the original request
   * with a custom set of filters.  Note in merge service, if the filters
   * passed here do not result in a subset of the subset resulting from the
   * original filters, merge logic will fail.  e.g. if a participant stream
   * overrides filters to include participants whose households are NOT in
   * the household stream, we cannot inherit household vars (because the
   * household of those participants does not exist in the household stream.
   *
   * @param overriddenFilters filters to override or null if no override necessary
   * @return this stream spec
   */
  public StreamSpec setFiltersOverride(List<APIFilter> overriddenFilters) {
    _overriddenFilters = overriddenFilters;
    return this;
  }

  public StreamSpec addVar(VariableSpec variableSpec) {
    if (variableSpec != null) {
      add(variableSpec);
    }
    return this;
  }

  public <T extends VariableSpec> StreamSpec addVars(Collection<T> variableSpecs) {
    if (variableSpecs != null) {
      addAll(variableSpecs);
    }
    return this;
  }

  @Override
  public String toString() {
    List<String> vars = stream()
        .map(JsonUtil::serializeObject)
        .collect(Collectors.toList());
    return new JSONObject()
      .put("name", _streamName)
      .put("entityId", _entityId)
      .put("variables", vars)
      .toString(2);
  }

  public String toString(int indentLength) {
      String indent = " ".repeat(indentLength);
      return indent + "{" + NL +
          indent + "  name: " + getStreamName() + NL +
          indent + "  entityId: " + getEntityId() + NL +
          indent + "  vars: [ " + stream().map(VariableDef::toDotNotation).collect(Collectors.joining()) + " ]" + NL +
          indent + "}";
  }
}
