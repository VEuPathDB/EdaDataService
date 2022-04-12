package org.veupathdb.service.eda.ds.plugin.sample;

import jakarta.ws.rs.BadRequestException;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.CollectionDef;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.CollectionSpec;
import org.veupathdb.service.eda.generated.model.TestCollectionsPostRequest;
import org.veupathdb.service.eda.generated.model.TestCollectionsSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class TestCollectionPlugin extends AbstractPlugin<TestCollectionsPostRequest, TestCollectionsSpec> {

  private CollectionDef _collectionDef;

  @Override
  protected Class<TestCollectionsSpec> getVisualizationSpecClass() {
    return TestCollectionsSpec.class;
  }

  @Override
  protected void validateVisualizationSpec(TestCollectionsSpec pluginSpec) throws ValidationException {
    CollectionSpec spec = pluginSpec.getCollectionSpec();
    EntityDef entity = getReferenceMetadata().getEntity(spec.getEntityId())
        .orElseThrow(() -> new BadRequestException("Passed entity ID " + spec.getEntityId() + " is invalid."));
    _collectionDef = entity.getCollection(spec)
        .orElseThrow(() -> new BadRequestException("Entity " + entity.getId() + " does not contain collection " + spec.getCollectionId()));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(TestCollectionsSpec pluginSpec) {
    return List.of(new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, _collectionDef.getEntityId())
        .addVars(_collectionDef.getMemberVariables()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    IoUtil.transferStream(out, dataStreams.values().iterator().next());
  }
}
