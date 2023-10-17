package org.veupathdb.service.eda.common.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.gusdb.fgputil.Tuples;
import org.gusdb.fgputil.client.ClientUtil;
import org.gusdb.fgputil.client.RequestFailure;
import org.gusdb.fgputil.client.ResponseFuture;
import org.gusdb.fgputil.functional.Either;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.web.MimeTypes;
import org.veupathdb.service.eda.common.client.spec.EdaMergingSpecValidator;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.client.spec.StreamSpecValidator;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.generated.model.APIFilter;
import org.veupathdb.service.eda.generated.model.ComputeSpecForMerging;
import org.veupathdb.service.eda.generated.model.ComputeSpecForMergingImpl;
import org.veupathdb.service.eda.generated.model.DerivedVariableBulkMetadataRequest;
import org.veupathdb.service.eda.generated.model.DerivedVariableBulkMetadataRequestImpl;
import org.veupathdb.service.eda.generated.model.DerivedVariableMetadata;
import org.veupathdb.service.eda.generated.model.DerivedVariableSpec;
import org.veupathdb.service.eda.generated.model.MergedEntityTabularPostRequest;
import org.veupathdb.service.eda.generated.model.MergedEntityTabularPostRequestImpl;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.merge.core.MergeRequestProcessor;
import org.veupathdb.service.eda.merge.core.request.MergedTabularRequestResources;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

public class EdaMergingClient extends StreamingDataClient {

  public EdaMergingClient(String serviceBaseUrl, Entry<String, String> authHeader) {
    super(serviceBaseUrl, authHeader);
  }

  @Override
  public StreamSpecValidator getStreamSpecValidator() {
    return new EdaMergingSpecValidator();
  }

  @Override
  public String varToColumnHeader(VariableSpec var) {
    return VariableDef.toDotNotation(var);
  }

  @Override
  public ResponseFuture getTabularDataStream(
      ReferenceMetadata metadata,
      List<APIFilter> subset,
      StreamSpec spec) throws ProcessingException {
    return getTabularDataStream(metadata, subset, Collections.emptyList(), Optional.empty(), spec);
  }

  public ResponseFuture getTabularDataStream(
      ReferenceMetadata metadata,
      List<APIFilter> subset,
      List<DerivedVariableSpec> derivedVariableSpecs,
      Optional<Tuples.TwoTuple<String,Object>> computeInfoOpt,
      StreamSpec spec) throws ProcessingException {

    // build request object
    MergedEntityTabularPostRequest request = new MergedEntityTabularPostRequestImpl();
    request.setStudyId(metadata.getStudyId());
    request.setFilters(spec.getFiltersOverride().orElse(subset));
    request.setEntityId(spec.getEntityId());
    request.setDerivedVariables(derivedVariableSpecs);
    request.setOutputVariables(spec);

    // if asked to include computed vars, do some validation before trying
    if (spec.isIncludeComputedVars()) {
      // a compute name and config must be provided
      Tuples.TwoTuple<String,Object> computeInfo = computeInfoOpt.orElseThrow(() -> new RuntimeException(
          "Computed vars requested but no compute associated with this visualization"));
      // compute entity must be the same as, or a parent of, the target entity
      /* FIX on 12/21/22: compute config does not necessarily know output entity so cannot validate here without
                          first requesting the metadata object; may do that in the future but for now, skip
      String computeEntityId = computeInfo.getSecond().getOutputEntityId();
      EntityDef target = metadata.getEntity(spec.getEntityId()).orElseThrow();
      if (!computeEntityId.equals(target.getId()) &&
          metadata.getAncestors(target).stream().filter(ent -> ent.getId().equals(computeEntityId)).findFirst().isEmpty()) {
        throw new RuntimeException("Computed entity must be the same as, or an ancestor of, the target entity of the merged stream.");
      }*/
      ComputeSpecForMerging computeSpec = new ComputeSpecForMergingImpl();
      computeSpec.setComputeName(computeInfo.getFirst());
      computeSpec.setComputeConfig(computeInfo.getSecond());
      request.setComputeSpec(computeSpec);
    }
//    try {
//      final MergeRequestProcessor m = new MergeRequestProcessor(new MergedTabularRequestResources(request, getAuthHeaderMap().entrySet().stream().findFirst().get()));
//      m.createMergedResponseSupplier().accept();
//
//    } catch (Exception e) {
//
//    }
    // make request
    return ClientUtil.makeAsyncPostRequest(getUrl("/merging-internal/query"), request, MimeTypes.TEXT_TABULAR, getAuthHeaderMap());
  }

  public List<DerivedVariableMetadata> getDerivedVariableMetadata(String studyId, List<DerivedVariableSpec> derivedVariableSpecs) {

    // return empty if given empty
    if (derivedVariableSpecs.isEmpty()) return Collections.emptyList();

    // create the request
    DerivedVariableBulkMetadataRequest request = new DerivedVariableBulkMetadataRequestImpl();
    request.setStudyId(studyId);
    request.setDerivedVariables(derivedVariableSpecs);

    // submit the request
    String url = "/merging-internal/derived-variables/metadata/variables";
    ResponseFuture responseFuture = ClientUtil.makeAsyncPostRequest(getUrl(url), request, MediaType.APPLICATION_JSON, getAuthHeaderMap());

    // read and parse response
    try {
      Either<InputStream, RequestFailure> response = responseFuture.getEither();
      Function<RequestFailure,RuntimeException> failureHandler = fail ->
        fail.getStatusType().getFamily().equals(Response.Status.Family.CLIENT_ERROR)
            ? new BadRequestException(fail.getResponseBody())
            : new RuntimeException("Failed to get derived variable metadata. " + fail.getResponseBody());
      try (InputStream in = response.leftOrElseThrowWithRight(failureHandler)) {
        ObjectMapper objectMapper = JsonUtil.Jackson;
        return objectMapper.readValue(in, objectMapper.getTypeFactory()
            .constructCollectionType(List.class, DerivedVariableMetadata.class));
      }
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to request derived variable metadata from merging service.", e);
    }
  }

}
