package org.veupathdb.service.eda.ds.plugin.correlation;

import java.util.List;

import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.plugin.correlation.AbstractCorrelationBipartiteNetwork;
import org.veupathdb.service.eda.generated.model.*;

public class CorrelationBipartitenetworkPlugin extends AbstractCorrelationBipartiteNetwork<CorrelationBipartitenetworkPostRequest, CorrelationConfig> {

  @Override
  protected ClassGroup getTypeParameterClasses() {
    return new ClassGroup(CorrelationBipartitenetworkPostRequest.class, CorrelationNetworkSpec.class, CorrelationConfig.class);
  }

  @Override
  public List<String> getProjects() {
    return List.of(AppsMetadata.MICROBIOME_PROJECT);
  }

}
