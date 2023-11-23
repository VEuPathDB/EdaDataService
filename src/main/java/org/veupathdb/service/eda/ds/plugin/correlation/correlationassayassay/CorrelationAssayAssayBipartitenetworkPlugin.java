package org.veupathdb.service.eda.ds.plugin.correlation.correlationassaymetadata;

import org.veupathdb.service.eda.ds.plugin.correlation.AbstractCorrelationBipartiteNetwork;
import org.veupathdb.service.eda.generated.model.*;

public class CorrelationAssayAssayBipartitenetworkPlugin extends AbstractCorrelationBipartiteNetwork {

  @Override
  public List<String> getProjects() {
    return List.of(AppsMetadata.MICROBIOME_PROJECT);
  }

}
