package org.veupathdb.service.eda.ds.plugin.correlation;

import java.util.List;

import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.plugin.correlation.AbstractCorrelationBipartiteNetwork;
import org.veupathdb.service.eda.generated.model.*;

public class CorrelationAssayMetadataBipartitenetworkPlugin extends AbstractCorrelationBipartiteNetwork<CorrelationAssayMetadataBipartitenetworkPostRequest, CorrelationAssayMetadataConfig> {

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new ClassGroup(CorrelationAssayMetadataBipartitenetworkPostRequest.class, CorrelationNetworkSpec.class, CorrelationAssayMetadataConfig.class);
  }

  @Override
  public List<String> getProjects() {
    return List.of(AppsMetadata.MICROBIOME_PROJECT);
  }

}
