package org.veupathdb.service.eda.ds.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import org.gusdb.fgputil.functional.Either;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.generated.model.APIFilter;
import org.veupathdb.service.eda.generated.model.APIStudyDetail;
import org.veupathdb.service.eda.generated.model.DerivedVariable;
import org.veupathdb.service.eda.generated.model.EntityTabularPostRequest;
import org.veupathdb.service.eda.generated.model.EntityTabularPostRequestImpl;
import org.veupathdb.service.eda.generated.model.StudyIdGetResponse;

public class EdaClient {

  public static APIStudyDetail getStudy(String studyId) {
    return NetworkUtils.getResponseObject("/studies/" + studyId, StudyIdGetResponse.class).getStudy();
  }

  public static InputStream getDataStream(
      APIStudyDetail study,
      List<APIFilter> subset,
      List<DerivedVariable> derivedVariables,
      StreamSpec spec) {
    EntityTabularPostRequest request = new EntityTabularPostRequestImpl();
    request.setFilters(subset);
    request.setOutputVariableIds(spec.stream()
      .map(var -> var.getVariableId())
      .collect(Collectors.toList()));
    String url = Resources.SUBSETTING_SERVICE_URL + "/studies/" + study.getId() + "/entities/" + spec.getEntityId() + "/tabular";

    try {
      Either<InputStream, NetworkUtils.RequestFailure> result = NetworkUtils.makePostRequest(url, request, "text/tabular");
      if (result.isLeft()) return result.getLeft();
      throw new RuntimeException(result.getRight().toString());
    }
    catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
