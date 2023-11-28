package org.veupathdb.service.eda.ds.plugin.correlation.correlationassaymetadata;

import java.util.List;

import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.plugin.correlation.AbstractCorrelationBipartiteNetwork;
import org.veupathdb.service.eda.generated.model.*;

public class CorrelationAssayMetadataBipartitenetworkPlugin extends AbstractCorrelationBipartiteNetwork<CorrelationAssayMetadataBipartitenetworkPostRequest, Correlation1Collection> {

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new ClassGroup(CorrelationAssayMetadataBipartitenetworkPostRequest.class, CorrelationBipartitenetworkSpec.class, Correlation1Collection.class);
  }

  @Override
  public List<String> getProjects() {
    return List.of(AppsMetadata.MICROBIOME_PROJECT);
  }

}
