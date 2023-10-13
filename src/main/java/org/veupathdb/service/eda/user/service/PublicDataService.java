package org.veupathdb.service.eda.user.service;

import org.veupathdb.service.eda.generated.model.AnalysisSummaryWithUser;
import org.veupathdb.service.eda.generated.resources.PublicAnalysesProjectId;
import org.veupathdb.service.eda.user.model.AccountDbData;
import org.veupathdb.service.eda.user.model.ProvenancePropsLookup;
import org.veupathdb.service.eda.user.model.UserDataFactory;

import java.util.List;

public class PublicDataService implements PublicAnalysesProjectId {

  @Override
  public GetPublicAnalysesByProjectIdResponse getPublicAnalysesByProjectId(String projectId) {
    UserDataFactory dataFactory = new UserDataFactory(projectId);
    List<AnalysisSummaryWithUser> publicAnalyses = dataFactory.getPublicAnalyses();
    ProvenancePropsLookup.assignCurrentProvenanceProps(dataFactory, publicAnalyses);
    return GetPublicAnalysesByProjectIdResponse.respond200WithApplicationJson(
        new AccountDbData().populateOwnerData(publicAnalyses));
  }
}
