package org.veupathdb.service.edads.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;
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
      Optional<List<APIFilter>> subset,
      Optional<List<DerivedVariable>> derivedVariables,
      StreamSpec spec) {
    EntityTabularPostRequest request = new EntityTabularPostRequestImpl();
    subset.ifPresent(filters -> request.setFilters(filters));
    request.setOutputVariableIds(spec);
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
