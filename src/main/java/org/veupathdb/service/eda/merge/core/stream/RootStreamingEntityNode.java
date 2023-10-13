package org.veupathdb.service.eda.merge.core.stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.collection.InitialSizeStringMap;
import org.gusdb.fgputil.iterator.CloseableIterator;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.generated.model.APIFilter;
import org.veupathdb.service.eda.generated.model.VariableMapping;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.merge.core.derivedvars.DerivedVariableFactory;
import org.veupathdb.service.eda.merge.core.request.ComputeInfo;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.gusdb.fgputil.FormatUtil.NL;

/**
 * Special instance of StreamingEntityNode which is used as the root of the entity stream tree.  As the root, its
 * additional responsibilities are:
 *
 * 1. Return an ordered row containing only the originally requested columns (all other nodes provide "extra"
 *    rows and are unordered)
 * 2. Collects StreamSpecs from the tree and logs them before returning
 * 3. Is the 'public' distributor of the data streams to the tree
 * 4. Applies computed vars if necessary.  There can be only one computed entity and it must be this node's
 *    entity (the target entity) or an ancestor
 * 5. Provides a method indicating whether any data manipulation is required (i.e. if the caller can get away
 *    with streaming its single tabular stream (from subsetting) without using the entity tree at all (much quicker)
 */
public class RootStreamingEntityNode extends StreamingEntityNode {

  private static final Logger LOG = LogManager.getLogger(RootStreamingEntityNode.class);

  // special name for the stream of computed tabular data (only ever one compute per merge request)
  public static final String COMPUTED_VAR_STREAM_NAME = "__COMPUTED_VAR_STREAM__";

  private final String[] _outputVars;
  private final InitialSizeStringMap _outputRow;

  private final Optional<EntityStream> _computeStreamProcessor;
  private final boolean _computeEntityMatchesOurs;

  public RootStreamingEntityNode(
      EntityDef targetEntity,
      List<VariableDef> outputVarDefs,
      List<APIFilter> subsetFilters,
      ReferenceMetadata metadata,
      DerivedVariableFactory derivedVariableFactory,
      Optional<ComputeInfo> computeInfo) throws ValidationException {

    super(targetEntity, outputVarDefs, subsetFilters, metadata, derivedVariableFactory, INITIAL_DEPENDENCY_DEPTH);

    // create stream spec for compute request and consuming processor
    Optional<StreamSpec> computeStreamSpec = computeInfo.flatMap(info -> getComputeStreamSpec(info.getVariables()));
    _computeStreamProcessor = computeStreamSpec.map(spec -> new EntityStream(metadata).setStreamSpec(spec));
    _computeEntityMatchesOurs = computeStreamSpec.map(spec -> spec.getEntityId().equals(targetEntity.getId())).orElse(false);

    // header names for values we will return (computed vars go at the end)
    List<VariableSpec> fullOutputVarDefs = new ArrayList<>(outputVarDefs); // make a copy
    computeStreamSpec.ifPresent(fullOutputVarDefs::addAll);

    _outputVars = getOrderedOutputColumns(fullOutputVarDefs);
    LOG.info("Root stream final output vars: " + String.join(", ", _outputVars));
    _outputRow = new InitialSizeStringMap.Builder(_outputVars).build();
  }

  private String[] getOrderedOutputColumns(List<VariableSpec> fullOutputVarDefs) throws ValidationException {
    List<String> outputCols = VariableDef.toDotNotation(fullOutputVarDefs);
    Set<String> distinctnessCheck = new HashSet<>();
    for (String outputCol : outputCols) {
      if (distinctnessCheck.contains(outputCol)) {
        throw new ValidationException("Output variables (columns) must be distinct.  Column '" + outputCol + "' is specified more than once.");
      }
      distinctnessCheck.add(outputCol);
    }
    return outputCols.toArray(new String[0]);
  }

  private static Optional<StreamSpec> getComputeStreamSpec(List<VariableMapping> varMappings) {

    // if no computed vars present, nothing to do
    if (varMappings.isEmpty()) return Optional.empty();

    // create variable specs from computed var metadata
    List<VariableSpec> computedVars = new ArrayList<>();
    varMappings.forEach(varMapping -> {
      if (varMapping.getIsCollection()) {
        // for collection vars, expect columns for each member
        computedVars.addAll(varMapping.getMembers());
      }
      else {
        // for non-collections, add the mapping's spec
        computedVars.add(varMapping.getVariableSpec());
      }
    });

    return Optional.of(new StreamSpec(COMPUTED_VAR_STREAM_NAME,
        varMappings.get(0).getVariableSpec().getEntityId()).addVars(computedVars));
  }

  @Override
  public List<StreamSpec> getRequiredStreamSpecs() {
    List<StreamSpec> streams = super.getRequiredStreamSpecs();
    _computeStreamProcessor.ifPresent(stream ->
        streams.add(stream.getStreamSpec()));
    LOG.info("Created "+ streams.size() + " stream specs needed to create this response: " + NL +
        streams.stream().map(spec -> spec.toString() + NL + "  filtersOverride: " +
            spec.getFiltersOverride().map(JsonUtil::serializeObject).orElse("none")
        ).collect(Collectors.joining(NL)));
    return streams;
  }

  @Override
  public boolean requiresNoDataManipulation() {
    return super.requiresNoDataManipulation() && _computeStreamProcessor.isEmpty();
  }

  @Override
  public void acceptDataStreams(Map<String, CloseableIterator<Map<String, String>>> dataStreams) {
    _computeStreamProcessor.ifPresent(s -> s.acceptDataStreams(dataStreams));
    super.acceptDataStreams(dataStreams);
  }

  @Override
  public Map<String,String> next() {
    // get row generated by superclass (native, inherited, and derived vars)
    Map<String, String> row = super.next();

    //LOG.debug("Root node row before picking output vars: " + FormatUtil.prettyPrint(row, FormatUtil.Style.SINGLE_LINE));

    // apply computed vars
    _computeStreamProcessor.ifPresent(computeStream -> applyCompute(row, computeStream));

    // return only requested vars and in the correct order
    _outputRow.clear();
    for (String col : _outputVars) {
      _outputRow.put(col, row.get(col));
    }
    return _outputRow;
  }

  private void applyCompute(Map<String, String> row, EntityStream computeStream) {

    // different logic if computed entity is target entity vs ancestor
    if (_computeEntityMatchesOurs) {
      // should have exactly one compute row per target row since using the same subset
      // make sure a computed row is present for this row
      if (!computeStream.hasNext())
        throw new IllegalStateException("Computed data column does not have enough rows for this subset");
      // read the row
      Map<String,String> nextComputedRow = computeStream.next();
      // make sure ID matches
      if (!row.get(getEntityIdColumnName()).equals(nextComputedRow.get(getEntityIdColumnName()))) {
        throw new IllegalStateException("Computed row entity ID '" + nextComputedRow.get(getEntityIdColumnName()) +
            " does not match expected ID " + row.get(getEntityIdColumnName()));
      }
      // add values to row
      row.putAll(nextComputedRow);
    }
    else {
      // treat compute stream like any other ancestor stream
      applyAncestorVars(computeStream, row);
    }
  }

  @Override
  public String toString() {
    return NL + "RootStream {" + NL +
        "  outputVars: [ " + String.join(", ", _outputVars) + " ]," + NL +
        "  computeStreamEntityMatchesOurs: " + _computeEntityMatchesOurs + NL +
        "  computeStream:" + _computeStreamProcessor.map(c -> NL + c.toString(2)).orElse(" <none>") + NL +
        "  nodeProperties:" + NL + toString(2) + NL +
        "}" + NL;
  }

  public String[] getOrderedOutputVars() {
    return _outputVars;
  }
}
