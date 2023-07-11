package org.veupathdb.service.eda.ds.plugin.standalonemap;

import com.google.common.collect.Sets;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.MapBubbleSpecification;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.CollectionSpec;
import org.veupathdb.service.eda.generated.model.CollectionSpecImpl;
import org.veupathdb.service.eda.generated.model.MemberVariable;
import org.veupathdb.service.eda.generated.model.QuantitativeOverlayConfig;
import org.veupathdb.service.eda.generated.model.StandaloneCollectionMapMarkerPostRequest;
import org.veupathdb.service.eda.generated.model.StandaloneCollectionMapMarkerPostResponse;
import org.veupathdb.service.eda.generated.model.StandaloneCollectionMapMarkerPostResponseImpl;
import org.veupathdb.service.eda.generated.model.StandaloneCollectionMapMarkerSpec;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.veupathdb.service.eda.ds.metadata.AppsMetadata.VECTORBASE_PROJECT;

public class CollectionMapMarkersPlugin extends AbstractEmptyComputePlugin<StandaloneCollectionMapMarkerPostRequest, StandaloneCollectionMapMarkerSpec> {

  @Override
  public List<String> getProjects() {
    return List.of(VECTORBASE_PROJECT);
  }

  @Override
  public ConstraintSpec getConstraintSpec() {
    // TODO stub implementation
    return new ConstraintSpec();
  }


  @Override
  protected AbstractPlugin<StandaloneCollectionMapMarkerPostRequest, StandaloneCollectionMapMarkerSpec, Void>.ClassGroup getTypeParameterClasses() {
    return new ClassGroup(StandaloneCollectionMapMarkerPostRequest.class, StandaloneCollectionMapMarkerSpec.class, Void.class);
  }

  @Override
  protected void validateVisualizationSpec(StandaloneCollectionMapMarkerSpec pluginSpec) throws ValidationException {
    final List<VariableDef> allMembers = getUtil().getCollectionMembers(pluginSpec.getCollection());
    final Set<String> selectedMemberIds = pluginSpec.getSelectedMemberVariables().stream()
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
    // TODO rest of validation
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(StandaloneCollectionMapMarkerSpec pluginSpec) {
    // TODO stub implementation
    return List.of(new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, pluginSpec.getOutputEntityId()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    // TODO stub implementation
    // Construct response, serialize and flush output

    final StandaloneCollectionMapMarkerPostResponse response = new StandaloneCollectionMapMarkerPostResponseImpl();
    JsonUtil.Jackson.writeValue(out, response);
    out.flush();
  }
}
