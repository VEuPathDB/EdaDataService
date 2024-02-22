package org.veupathdb.service.eda.ds.plugin.correlation.correlationassayassay;

import java.util.List;

import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.plugin.correlation.AbstractCorrelationBipartiteNetwork;
import org.veupathdb.service.eda.generated.model.*;

public class CorrelationAssayAssayBipartitenetworkPlugin extends AbstractCorrelationBipartiteNetwork<CorrelationAssayAssayBipartitenetworkPostRequest, Correlation2Collections> {

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new ClassGroup(CorrelationAssayAssayBipartitenetworkPostRequest.class, CorrelationNetworkSpec.class, Correlation2Collections.class);
  }

  @Override
  public List<String> getProjects() {
    return List.of(AppsMetadata.MICROBIOME_PROJECT);
  }

}
