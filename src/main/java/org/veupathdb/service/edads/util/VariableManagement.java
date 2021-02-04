package org.veupathdb.service.edads.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.veupathdb.service.edads.generated.model.APIEntity;
import org.veupathdb.service.edads.generated.model.APIStudyDetail;
import org.veupathdb.service.edads.generated.model.APIVariableType;
import org.veupathdb.service.edads.generated.model.DerivedVariable;

public class VariableManagement {

  /**
   * Generates a map from entity name to entity def, which is just the entity name
   * and a collection of all available variables on that entity (native, inherited,
   * and derived).  Once these are determined, the tree structure does not matter
   * from the plugin's perspective.
   *
   * @param study study for which entity variables should be supplemented
   * @param allSpecifiedDerivedVariables derived var definitions sent by client
   * @return map of entity ID to entity defs containing all available vars on those entities
   */
  public static Map<String, EntityDef> supplementEntities(APIStudyDetail study, List<DerivedVariable> allSpecifiedDerivedVariables) {
    return supplementEntities(study.getRootEntity(), allSpecifiedDerivedVariables, new ArrayList<>());
  }

  /**
   * Creates a single entity def from the passed entity and adds it to the map
   * with all its available variables (native, inherited, and derived).  Then
   * recursively calls itself to populate the map with its children.
   *
   * @param entity an entity to convert to a def, supplement vars of, and add to returned map
   * @param allSpecifiedDerivedVariables derived var definitions sent by client
   * @param parentNativeVars native vars supplied by parent entities
   * @return map of entity ID to entity defs containing all available vars on those entities
   */
  private static Map<String, EntityDef> supplementEntities(APIEntity entity,
      List<DerivedVariable> allSpecifiedDerivedVariables, List<VariableDef> parentNativeVars) {
    Map<String, EntityDef> entities = new HashMap<>();
    EntityDef entityDef = new EntityDef(entity.getId());

    // add inherited variables from parent
    parentNativeVars.stream()
      .forEach(vd -> entityDef.add(vd));

    // process this entity's native vars
    entity.getVariables().stream()
      .filter(var -> !var.getType().equals(APIVariableType.CATEGORY))
      .map(var -> new VariableDef(entity.getId(), var.getId(), var.getType()))
      .forEach(vd -> {
        // add variables for this entity
        entityDef.add(vd);

        // add this entity's native vars to parentNativeVars list
        parentNativeVars.add(vd);
      });

    // add derived variables for this entity
    //  (for now, can only use derived vars declared on the current entity, not its parents)
    allSpecifiedDerivedVariables.stream()
      // only derived vars for this entity
      .filter(dr -> dr.getEntityId().equals(entity.getId()))
      // skip if entity already contains the variable; will throw later
      .filter(dr -> !entityDef.hasVariable(dr))
      .map(dr -> new VariableDef(entity.getId(), dr.getVariableId(), dr.getVariableType()))
      .forEach(vd -> entityDef.add(vd));

    // add this entity to the map
    entities.put(entityDef.getId(), entityDef);

    // add child entities to map
    for (APIEntity childEntity : entity.getChildren()) {
      // create new array list each time; don't want branches of entity tree polluting each other
      entities.putAll(supplementEntities(childEntity, allSpecifiedDerivedVariables, new ArrayList<>(parentNativeVars)));
    }

    return entities;
  }
}
