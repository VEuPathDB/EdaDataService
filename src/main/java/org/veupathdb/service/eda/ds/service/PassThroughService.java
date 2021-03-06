package org.veupathdb.service.eda.ds.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.client.RequestFailure;
import org.gusdb.fgputil.functional.Either;
import org.gusdb.fgputil.functional.FunctionalInterfaces.FunctionWithException;
import org.veupathdb.lib.container.jaxrs.errors.UnprocessableEntityException;
import org.veupathdb.service.eda.common.client.EdaSubsettingClient;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.generated.model.EntityCountPostRequest;
import org.veupathdb.service.eda.generated.model.EntityCountPostResponseStream;
import org.veupathdb.service.eda.generated.model.EntityIdGetResponseStream;
import org.veupathdb.service.eda.generated.model.StudiesGetResponseStream;
import org.veupathdb.service.eda.generated.model.StudyIdGetResponseStream;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostRequest;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostResponseStream;
import org.veupathdb.service.eda.generated.resources.Studies;

import static org.gusdb.fgputil.functional.Functions.cSwallow;

public class PassThroughService implements Studies {

  private interface DataProducer extends
      FunctionWithException<EdaSubsettingClient, Either<InputStream, RequestFailure>> {}

  private Consumer<OutputStream> buildStreamer(DataProducer dataProducer) {
    try {
      // get result
      Either<InputStream, RequestFailure> result =
          dataProducer.apply(new EdaSubsettingClient(Resources.SUBSETTING_SERVICE_URL));
      // if successful, stream response as our response
      if (result.isLeft()) {
        return cSwallow(out -> IoUtil.transferStream(out, result.getLeft()));
      }
      // if unsuccessful, try to mirror the failure
      RequestFailure fail = result.getRight();
      switch(fail.getStatusType().getStatusCode()) {
        case 400: throw new BadRequestException(fail.getResponseBody());
        case 404: throw new NotFoundException(fail.getResponseBody());
        case 405: throw new NotAllowedException(fail.getResponseBody());
        case 422: throw new UnprocessableEntityException(fail.getResponseBody());
        default: throw new InternalServerErrorException(fail.getResponseBody());
      }
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to complete request", e);
    }
  }

  @Override
  public GetStudiesResponse getStudies() {
    return GetStudiesResponse.respond200WithApplicationJson(
        new StudiesGetResponseStream(buildStreamer(c -> c.getStudiesStream())));
  }

  @Override
  public GetStudiesByStudyIdResponse getStudiesByStudyId(String studyId) {
    return GetStudiesByStudyIdResponse.respond200WithApplicationJson(
        new StudyIdGetResponseStream(buildStreamer(c -> c.getStudyStream(studyId))));
  }

  @Override
  public GetStudiesEntitiesByStudyIdAndEntityIdResponse getStudiesEntitiesByStudyIdAndEntityId(String studyId, String entityId) {
    return GetStudiesEntitiesByStudyIdAndEntityIdResponse.respond200WithApplicationJson(
        new EntityIdGetResponseStream(buildStreamer(c -> c.getEntityStream(studyId, entityId))));
  }

  @Override
  public PostStudiesEntitiesCountByStudyIdAndEntityIdResponse postStudiesEntitiesCountByStudyIdAndEntityId(String studyId, String entityId, EntityCountPostRequest entity) {
    return PostStudiesEntitiesCountByStudyIdAndEntityIdResponse.respond200WithApplicationJson(
        new EntityCountPostResponseStream(buildStreamer(c -> c.getEntityCountStream(studyId, entityId, entity))));
  }

  @Override
  public PostStudiesEntitiesVariablesDistributionByStudyIdAndEntityIdAndVariableIdResponse postStudiesEntitiesVariablesDistributionByStudyIdAndEntityIdAndVariableId(String studyId, String entityId, String variableId, VariableDistributionPostRequest entity) {
    return PostStudiesEntitiesVariablesDistributionByStudyIdAndEntityIdAndVariableIdResponse.respond200WithApplicationJson(
        new VariableDistributionPostResponseStream(buildStreamer(c -> c.getVariableDistributionStream(studyId, entityId, variableId, entity))));
  }
}
