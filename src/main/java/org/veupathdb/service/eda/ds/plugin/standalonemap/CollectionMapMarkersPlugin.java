package org.veupathdb.service.eda.ds.plugin.standalonemap;

import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.ds.plugin.standalonemap.markers.MapBubbleSpecification;
import org.veupathdb.service.eda.generated.model.APIVariableType;
import org.veupathdb.service.eda.generated.model.StandaloneCollectionMapMarkerPostRequest;
import org.veupathdb.service.eda.generated.model.StandaloneCollectionMapMarkerPostResponse;
import org.veupathdb.service.eda.generated.model.StandaloneCollectionMapMarkerPostResponseImpl;
import org.veupathdb.service.eda.generated.model.StandaloneCollectionMapMarkerSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

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
    // TODO stub implementation
    return;
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
