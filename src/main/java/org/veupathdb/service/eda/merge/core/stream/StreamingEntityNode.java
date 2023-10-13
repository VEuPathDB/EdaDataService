package org.veupathdb.service.eda.merge.core.stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.functional.Functions;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.common.model.VariableSource;
import org.veupathdb.service.eda.generated.model.APIFilter;
import org.veupathdb.service.eda.merge.core.derivedvars.DerivedVariable;
import org.veupathdb.service.eda.merge.core.derivedvars.DerivedVariableFactory;
import org.veupathdb.service.eda.merge.core.derivedvars.Reduction;
import org.veupathdb.service.eda.merge.core.derivedvars.Transform;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.gusdb.fgputil.FormatUtil.NL;

/**
 * Child class of EntityStream which handles the generation and application of inherited and derived variables.  It is
 * through this class that the entity stream tree's structure is built; i.e. each instance is a node in the tree, and
 * data flows through these nodes from the leaves to the root, which is a special subclass instance
 * (RootStreamingEntityNode).
 */
public class StreamingEntityNode extends EntityStream {

  private static final Logger LOG = LogManager.getLogger(StreamingEntityNode.class);

  protected static final int INITIAL_DEPENDENCY_DEPTH = 0;
  private static final int MAX_ENTITY_DEPENDENCY_DEPTH = 15;

  private final String _entityIdColName;
  private final List<StreamingEntityNode> _ancestorStreams = new ArrayList<>();
  private final List<TwoTuple<Reduction,StreamingEntityNode>> _reductionStreams = new ArrayList<>();
  private final List<Transform> _orderedTransforms = new ArrayList<>();
  private final DerivedVariableFactory _derivedVariableFactory;

  public StreamingEntityNode(
      EntityDef targetEntity,
      List<VariableDef> outputVarDefs,
      List<APIFilter> subsetFilters,
      ReferenceMetadata metadata,
      DerivedVariableFactory derivedVariableFactory,
      int entityDependencyDepth
  ) throws ValidationException {
    super(metadata);
    _derivedVariableFactory = derivedVariableFactory;

    // check dependency depth
    if (entityDependencyDepth > MAX_ENTITY_DEPENDENCY_DEPTH) {
      throw new IllegalArgumentException("Maximum number of concurrent entity streams (" + MAX_ENTITY_DEPENDENCY_DEPTH + ") exceeded.");
    }

    // cache the name of the column used to identify records that match the current row
    _entityIdColName = VariableDef.toDotNotation(targetEntity.getIdColumnDef());

    // organize vars needed from ancestor entities; will use to create ancestor child nodes
    Map<String, List<VariableDef>> ancestorEntityOutputVars = Functions.getMapFromKeys(
        metadata.getAncestors(targetEntity).stream().map(EntityDef::getId).toList(), k -> new ArrayList<>());

    // create base stream spec for this node with passed filters (will add this entity's required native vars below)
    StreamSpec streamSpec = new StreamSpec(UUID.randomUUID().toString(), targetEntity.getId()).setFiltersOverride(subsetFilters);

    // explore the output vars to see what streams we need
    List<VariableDef> varsToConsider = new ArrayList<>(outputVarDefs); // this copy to be mutated
    Set<String> varsAlreadyHandled = new HashSet<>(); // to ensure loop ends and get comprehensive list of needed vars (transitive deps)

    // loop through requested vars
    for (int i = 0; i < varsToConsider.size(); i++) {
      VariableDef var = varsToConsider.get(i);

      // this ensures vars don't keep getting added to varsToConsider
      if (varsAlreadyHandled.contains(VariableDef.toDotNotation(var))) continue;
      varsAlreadyHandled.add(VariableDef.toDotNotation(var));

      if (var.getEntityId().equals(targetEntity.getId())) {
        // variable declared on this entity
        if (var.getSource() == VariableSource.ID) {
          // no need to specially request IDs; we get them for free
          continue;
        }
        if (var.getSource() == VariableSource.NATIVE) {
          // var is native to this entity; add to stream spec
          streamSpec.add(var);
        }
        else if (var.getSource() == VariableSource.DERIVED_TRANSFORM) {
          // find transform derived variable instance
          Transform transform = _derivedVariableFactory.getTransform(var).orElseThrow();
          // any vars the transform needs can be added to those needed by this entity overall
          varsToConsider.addAll(metadata.toVariableDefs(transform.getRequiredInputVars()));
        }
        else if (var.getSource() == VariableSource.DERIVED_REDUCTION) {
          // find reduction derived variable instance
          Reduction reduction = _derivedVariableFactory.getReduction(var).orElseThrow();
          StreamSpec reductionStreamSpec = reduction.getInputStreamSpec();
          _reductionStreams.add(new TwoTuple<>(reduction, new StreamingEntityNode(
              _metadata.getEntity(reductionStreamSpec.getEntityId()).orElseThrow(),
              _metadata.toVariableDefs(reductionStreamSpec),
              reductionStreamSpec.getFiltersOverride().orElse(subsetFilters),
              metadata,
              _derivedVariableFactory,
              entityDependencyDepth + 1
          )));
        }
        else {
          throw new IllegalArgumentException("Since variable " + var + " is of type " + var.getSource() + ", it should not be directly requested.");
        }
      }
      else if (ancestorEntityOutputVars.containsKey(var.getEntityId())) {
        // var is native to ancestor entity; add to that entry but skip IDs (we get them for free)
        if (var.getSource() != VariableSource.ID) {
          ancestorEntityOutputVars.get(var.getEntityId()).add(var);
        }
      }
      else {
        // bad variable; can't deliver vars from descendants or "over the hump" entities
        throw new IllegalArgumentException("Variable " + var + " cannot be returned on a stream of entity " + targetEntity.getId());
      }
    }

    // set the stream spec for this node's stream
    setStreamSpec(streamSpec);

    // build ancestor streams for inherited vars
    for (Entry<String, List<VariableDef>> ancestor : ancestorEntityOutputVars.entrySet()) {
      if (!ancestor.getValue().isEmpty()) {
        // at least one needed var; add entity stream
        _ancestorStreams.add(new StreamingEntityNode(
            _metadata.getEntity(ancestor.getKey()).orElseThrow(),
            ancestor.getValue(),
            subsetFilters,
            metadata,
            _derivedVariableFactory,
            entityDependencyDepth + 1
        ));
      }
    }

    // create a dependency-ordered list of transforms (most dependent vars last)
    for (DerivedVariable derivedVar : _derivedVariableFactory.getAllDerivedVars()) {
      if (derivedVar instanceof Transform                                           // is a transform
          && derivedVar.getEntity().getId().equals(derivedVar.getEntityId())        // assigned to this entity
          && varsAlreadyHandled.contains(VariableDef.toDotNotation(derivedVar))) {  // that has been directly or indirectly requested
        _orderedTransforms.add((Transform)derivedVar);
      }
    }
  }

  protected List<StreamSpec> getRequiredStreamSpecs() {
    List<StreamSpec> streamSpecs = new ArrayList<>();
    streamSpecs.add(getStreamSpec());
    _ancestorStreams.stream().map(StreamingEntityNode::getRequiredStreamSpecs).forEach(streamSpecs::addAll);
    _reductionStreams.stream().map(pair -> pair.getSecond().getRequiredStreamSpecs()).forEach(streamSpecs::addAll);
    return streamSpecs;
  }

  protected boolean requiresNoDataManipulation() {
    return _ancestorStreams.isEmpty()
        && _reductionStreams.isEmpty()
        && _derivedVariableFactory.getAllDerivedVars().isEmpty();
  }

  @Override
  public void acceptDataStreams(Map<String, InputStream> dataStreams) {
    // order matters here; incoming data must be initialized before this node
    //   initializes its first row or required columns will not be present
    _ancestorStreams.forEach(s -> s.acceptDataStreams(dataStreams));
    _reductionStreams.forEach(pair -> pair.getSecond().acceptDataStreams(dataStreams));
    super.acceptDataStreams(dataStreams);
  }

  @Override
  protected Map<String, String> applyDerivedVars(Map<String, String> row) {

    // inherit vars from ancestor streams
    for (StreamingEntityNode ancestorStream : _ancestorStreams) {
      applyAncestorVars(ancestorStream, row);
    }

    // apply descendant stream values for calculation of each reduction derived var
    applyReductions(row);

    // apply transforms to this row
    for (Transform transform : _orderedTransforms) {
      row.put(transform.getColumnName(), transform.getValue(row));
    }

    return row;
  }

  protected void applyAncestorVars(EntityStream ancestorStream, Map<String,String> row) {

    String ancestorIdColName = ancestorStream.getEntityIdColumnName();
    Predicate<Map<String,String>> isMatch = r -> r.get(ancestorIdColName).equals(row.get(ancestorIdColName));
    Optional<Map<String,String>> ancestorRow = ancestorStream.getPreviousRowIf(isMatch);

    // loop through ancestor rows until we find a match for ours
    while (ancestorStream.hasNext() && ancestorRow.isEmpty()) {
      // this row is a member of a new ancestor of this entity; move to the next row
      ancestorStream.next(); // throws away the previous ancestor row because it didn't match ours
      ancestorRow = ancestorStream.getPreviousRowIf(isMatch);
    }

    if (ancestorRow.isEmpty()) {
      // Still empty and ancestor stream is exhausted.  We expect every target entity row to
      // have a matching row in each ancestor entity's stream.  Not having one is a fatal error.
      throw new IllegalStateException("Ancestor stream '" + ancestorStream.getStreamSpec().getEntityId() +
          "' could not provide a row matching '" + ancestorIdColName + "' with value '" + row.get(ancestorIdColName) + "'.");
    }

    row.putAll(ancestorRow.get());
  }

  private void applyReductions(Map<String, String> row) {

    // predicate tells the descendant streams how to match our row's ID
    Predicate<Map<String,String>> isMatch = r -> r.get(_entityIdColName).equals(row.get(_entityIdColName));

    // pull reduction derived vars from each descendant stream
    for (TwoTuple<Reduction,StreamingEntityNode> reduction : _reductionStreams) {

      // create a reducer for this row
      Reduction.Reducer reducer = reduction.getFirst().createReducer();

      // read rows until no longer matching this entity's ID column
      Optional<Map<String,String>> descendantRow = reduction.getSecond().getNextRowIf(isMatch);
      while (descendantRow.isPresent()) {

        // apply this row to the reducer
        reducer.addRow(descendantRow.get());

        // get next row (if matching)
        descendantRow = reduction.getSecond().getNextRowIf(isMatch);
      }

      // no more rows in the descendant stream match ours; build the derived var
      row.put(reduction.getKey().getColumnName(), reducer.getResultingValue());
    }
  }

  @Override
  public String toString() {
    return toString(0);
  }

  public String toString(int indentSize) {
    String indent = " ".repeat(indentSize);
    return indent + "{" + NL +
        indent + "  entityIdColName: " + _entityIdColName + NL +
        indent + "  entityStreamProps: " + NL +
        super.toString(indentSize + 2) + NL +
        indent + "  ancestorStreams: [" + NL +
        _ancestorStreams.stream().map(s -> s.toString(indentSize + 2) + NL).collect(Collectors.joining()) +
        indent + "  ]" + NL +
        indent + "  transforms: [" + NL +
        _orderedTransforms.stream().map(t -> indent + "  " + t + NL).collect(Collectors.joining()) +
        indent + "  ]" + NL +
        indent + "  reductions: [" + NL +
        _reductionStreams.stream().map(pair ->
            indent + "  {" + NL +
            indent + "    reduction: " + pair.getFirst() + NL +
            indent + "    stream:" + NL +
            indent + pair.getSecond().toString(indentSize + 4) + NL +
            indent + "  }" + NL).collect(Collectors.joining()) +
        indent + "  ]" + NL +
        indent + "}";
  }
}
