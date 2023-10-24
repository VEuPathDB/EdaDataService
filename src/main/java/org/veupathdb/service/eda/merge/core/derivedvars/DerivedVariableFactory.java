package org.veupathdb.service.eda.merge.core.derivedvars;

import jakarta.ws.rs.BadRequestException;
import org.gusdb.fgputil.validation.ValidationException;
import org.gusdb.fgputil.workflow.DependencyElement;
import org.gusdb.fgputil.workflow.DependencyResolver;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.generated.model.DerivedVariableSpec;
import org.veupathdb.service.eda.merge.plugins.Reductions;
import org.veupathdb.service.eda.merge.plugins.Transforms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.gusdb.fgputil.functional.Functions.wrapException;

public class DerivedVariableFactory {

  @FunctionalInterface
  public interface PluginBuilder<T extends DerivedVariable> {
    T build(ReferenceMetadata metadata, DerivedVariableSpec spec) throws ValidationException;
  }

  public static class PluginMap<T extends DerivedVariable> extends HashMap<String, PluginBuilder<T>> { }

  private final ReferenceMetadata _metadata;
  private final List<DerivedVariableSpec> _incomingSpecs;

  // maps from owning entity to each derived variable type
  private final Map<String, List<Transform>> _transforms = new HashMap<>();
  private final Map<String, List<Reduction>> _reductions = new HashMap<>();

  private final List<DerivedVariable> _allDerivedVariablesOrdered;

  public DerivedVariableFactory(
      ReferenceMetadata metadata,
      List<DerivedVariableSpec> derivedVariableSpecs) throws ValidationException {
    _metadata = metadata;
    _incomingSpecs = derivedVariableSpecs;
    List<DerivedVariable> unorderedDerivedVars = new ArrayList<>();
    // generates DerivedVariables and adds them to unorderedDerivedVars (not part of metadata yet)
    addDerivedVariableInstances(derivedVariableSpecs, unorderedDerivedVars);
    // ensure unique names
    if (unorderedDerivedVars.size() != unorderedDerivedVars.stream().map(VariableDef::toDotNotation).collect(Collectors.toSet()).size())
      throw new BadRequestException("Derived variable names are not unique.");
    _allDerivedVariablesOrdered = orderInstancesAndCheckCircularDependencies(unorderedDerivedVars);
  }

  private void addDerivedVariableInstances(List<DerivedVariableSpec> derivedVariableSpecs, List<DerivedVariable> allInstanceList) throws ValidationException {
    for (DerivedVariableSpec spec : derivedVariableSpecs) {
      // check name against transforms
      boolean found = addDerivedVariableInstance(spec, Transforms.getPlugins(), _transforms, allInstanceList);
      if (found) continue;
      // check name against reductions
      found = addDerivedVariableInstance(spec, Reductions.getPlugins(), _reductions, allInstanceList);
      if (!found)
        throw new BadRequestException("Unrecognized derived variable function name: " + spec.getFunctionName());
    }
  }

  private <T extends DerivedVariable> boolean addDerivedVariableInstance(
      DerivedVariableSpec spec,
      PluginMap<T> plugins,
      Map<String, List<T>> typedInstanceMap,
      List<DerivedVariable> allInstanceList) throws ValidationException {
    try {
      PluginBuilder<T> builder = plugins.get(spec.getFunctionName());
      if (builder == null) {
        return false;
      }

      // create the instance and look up the list to add it to
      T instance = builder.build(_metadata, spec);
      List<T> byEntityList = typedInstanceMap.computeIfAbsent(instance.getEntityId(), entityId -> new ArrayList<>());

      // only add if not already present (prevents infinite loop if circular dependency present)
      if (byEntityList.stream().noneMatch(i -> VariableDef.isSameVariable(i, instance))) {

        // add to the appropriate lists
        byEntityList.add(instance);
        allInstanceList.add(instance);

        // if this derived variable depends on internally declared derived vars, add them too
        List<DerivedVariableSpec> dependedDerivedVars = instance.getDependedDerivedVarSpecs();
        addDerivedVariableInstances(dependedDerivedVars, allInstanceList);
      }
      return true;
    }
    catch (ValidationException | RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to generate derived var instances from specs", e);
    }
  }

  @SafeVarargs
  public static <T extends AbstractDerivedVariable<?>> PluginMap<T> pluginsOf(Class<T> subtype, Class<? extends T>... implementations) {
    PluginMap<T> map = new PluginMap<>();
    for (Class<? extends T> plugin : implementations) {
      Supplier<T> supplier = () -> wrapException(() -> plugin.getConstructor().newInstance());
      map.put(supplier.get().getFunctionName(), (metadata, spec) -> {
        T obj = supplier.get();
        obj.init(metadata, spec);
        return obj;
      });
    }
    return map;
  }

  /**
   * Returns the complete list of derived variable instances (both from incoming specs and
   * possibly sub-vars declared by the derived variable plugins themselves.  This list will
   * be in dependency order; i.e. only later derived vars will depend on earlier derived
   * vars.  In addition, circular dependencies have already been checked for, and names
   * will be pre-validated for uniqueness among derived variables.  Name duplication with
   * existing vars in reference metadata will be flagged when inserted there.
   */
  public List<DerivedVariable> getAllDerivedVars() {
    return _allDerivedVariablesOrdered;
  }

  public Optional<Transform> getTransform(VariableDef var) {
    return getTransforms(_metadata.getEntity(var.getEntityId()).orElseThrow()).stream()
        .filter(t -> VariableDef.isSameVariable(t, var))
        .findAny();
  }

  public Optional<Reduction> getReduction(VariableDef var) {
    return getReductions(_metadata.getEntity(var.getEntityId()).orElseThrow()).stream()
        .filter(t -> VariableDef.isSameVariable(t, var))
        .findAny();
  }

  public List<Transform> getTransforms(EntityDef targetEntity) {
    return _transforms.computeIfAbsent(targetEntity.getId(), entityId -> new ArrayList<>());
  }

  public List<Reduction> getReductions(EntityDef targetEntity) {
    return _reductions.computeIfAbsent(targetEntity.getId(), entityId -> new ArrayList<>());
  }

  public List<DerivedVariableSpec> getDerivedVariableSpecs() {
    return _incomingSpecs;
  }

  private static class DerivedVariableNode implements DependencyElement<DerivedVariableNode> {

    public final DerivedVariable var;
    public Set<DerivedVariableNode> dependencies;

    public DerivedVariableNode(DerivedVariable var) {
      this.var = var;
      this.dependencies = new HashSet<>();
    }
    @Override
    public String getKey() {
      return VariableDef.toDotNotation(var);
    }

    @Override
    public Set<DerivedVariableNode> getDependedElements() {
      return dependencies;
    }

    @Override
    public void setDependentElements(List<DerivedVariableNode> dependentElements) {
      // nothing to do here - evaluation flow handled by entity streams so don't care who depends on this node
    }
  }

  private List<DerivedVariable> orderInstancesAndCheckCircularDependencies(List<DerivedVariable> unorderedInstanceList) {

    // create map of nodes
    Map<String,DerivedVariableNode> nodes = unorderedInstanceList.stream()
        .map(DerivedVariableNode::new)
        .collect(Collectors.toMap(DerivedVariableNode::getKey, Function.identity()));

    // add node direct dependencies to each node
    nodes.values().forEach(node ->
      node.dependencies.addAll(
          node.var.getDependedDerivedVarSpecs().stream()
              .map(VariableDef::toDotNotation)
              .map(nodes::get)
              .toList()));

    // This method figures out the dependency order for the derived vars; this is required
    // to properly validate depended vars as we load them into the reference metadata.  A
    // nice side effect is that if circular dependencies are found, an error is thrown.  The
    // dependent vars of each node are also calculated and assigned via setDependentElements()
    // above, but we do not need that information.
    List<DerivedVariableNode> instanceOrder = new DependencyResolver<DerivedVariableNode>()
        .addElements(nodes.values().toArray(new DerivedVariableNode[0]))
        .resolveDependencyOrder();

    // instanceOrder is in the order the nodes should be inserted (i.e. most dependent node
    // last).  Convert the node list to a sorted derived variable list.
    return instanceOrder.stream().map(node -> node.var).toList();
  }
}
