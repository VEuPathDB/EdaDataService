package org.veupathdb.service.eda.data.utils;

import com.google.common.collect.Sets;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.common.plugin.util.PluginUtil;
import org.veupathdb.service.eda.generated.model.CollectionSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ValidationUtils {

  public static void validateCollectionMembers(PluginUtil pluginUtil, CollectionSpec collectionSpec, List<VariableSpec> members) throws ValidationException {
    final List<VariableDef> allMembers = pluginUtil.getCollectionMembers(collectionSpec);
    final Set<String> selectedMemberIds = members.stream()
        .map(VariableSpec::getVariableId)
        .collect(Collectors.toSet());
    final Set<String> allMemberIds = allMembers.stream()
        .map(VariableDef::getVariableId)
        .collect(Collectors.toSet());
    final Set<String> invalidMemberIds = Sets.difference(selectedMemberIds, allMemberIds);
    if (!invalidMemberIds.isEmpty()) {
      throw new ValidationException("Specified member variables must belong to the specified collection. " +
          "The following were not found in collection: " + invalidMemberIds);
    }
  }
}
