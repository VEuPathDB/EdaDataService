package org.veupathdb.service.edads.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.gusdb.fgputil.functional.Either;
import org.veupathdb.service.edads.Resources;
import org.veupathdb.service.edads.generated.model.APIFilter;
import org.veupathdb.service.edads.generated.model.APIStudyDetail;
import org.veupathdb.service.edads.generated.model.DerivedVariable;
import org.veupathdb.service.edads.generated.model.EntityTabularPostRequest;
import org.veupathdb.service.edads.generated.model.EntityTabularPostRequestImpl;
import org.veupathdb.service.edads.generated.model.StudyIdGetResponse;
import org.veupathdb.service.edads.util.NetworkUtils.RequestFailure;
import org.veupathdb.service.edads.util.StreamSpec;

import static org.veupathdb.service.edads.util.NetworkUtils.getResponseObject;

public class EdaClient {

  public static APIStudyDetail getStudy(String studyId) {
    return getResponseObject("/studies/" + studyId, StudyIdGetResponse.class).getStudy();
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
      Either<InputStream, RequestFailure> result = NetworkUtils.makePostRequest(url, request, "text/tabular");
      if (result.isLeft()) return result.getLeft();
      throw new RuntimeException(result.getRight().toString());
    }
    catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
