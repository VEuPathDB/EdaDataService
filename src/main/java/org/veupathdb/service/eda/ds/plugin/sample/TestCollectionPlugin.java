package org.veupathdb.service.eda.ds.plugin.sample;

import jakarta.ws.rs.BadRequestException;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.CollectionDef;
import org.veupathdb.service.eda.ds.plugin.AbstractEmptyComputePlugin;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.generated.model.AbundanceScatterplotPostRequest;
import org.veupathdb.service.eda.generated.model.CollectionSpec;
import org.veupathdb.service.eda.generated.model.TestCollectionsPostRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class TestCollectionPlugin extends AbstractEmptyComputePlugin<TestCollectionsPostRequest, CollectionSpec> {

  @Override
  protected Class<TestCollectionsPostRequest> getVisualizationRequestClass() {
    return TestCollectionsPostRequest.class;
  }

  @Override
  protected Class<CollectionSpec> getVisualizationSpecClass() {
    return CollectionSpec.class;
  }

  @Override
  protected void validateVisualizationSpec(CollectionSpec spec) throws ValidationException {
    getReferenceMetadata().getCollection(spec).orElseThrow(
        () -> new BadRequestException("No collection exists for " + JsonUtil.serializeObject(spec)));
  }

  @Override
  protected List<StreamSpec> getRequestedStreams(CollectionSpec spec) {
    CollectionDef collection = getReferenceMetadata().getCollection(spec).orElseThrow();
    return List.of(new StreamSpec(DEFAULT_SINGLE_STREAM_NAME, collection.getEntityId())
        .addVars(collection.getMemberVariables()));
  }

  @Override
  protected void writeResults(OutputStream out, Map<String, InputStream> dataStreams) throws IOException {
    IoUtil.transferStream(out, dataStreams.values().iterator().next());
  }
}
