package org.veupathdb.service.eda.download;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.MapBuilder;
import org.veupathdb.lib.container.jaxrs.providers.UserProvider;
import org.veupathdb.lib.container.jaxrs.server.annotations.Authenticated;
import org.veupathdb.lib.container.jaxrs.server.middleware.CustomResponseHeadersFilter;
import org.veupathdb.service.eda.Resources;
import org.veupathdb.service.eda.common.auth.StudyAccess;
import org.veupathdb.service.eda.common.client.DatasetAccessClient;
import org.veupathdb.service.eda.common.client.DatasetAccessClient.StudyDatasetInfo;
import org.veupathdb.service.eda.generated.model.FileContentResponseStream;
import org.veupathdb.service.eda.generated.resources.Download;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

import static org.gusdb.fgputil.functional.Functions.cSwallow;

@Authenticated(allowGuests = true)
public class DownloadService implements Download {

  private static final Logger LOG = LogManager.getLogger(DownloadService.class);

  @Context
  ContainerRequest _request;

  @Override
  public GetDownloadByProjectAndStudyIdResponse getDownloadByProjectAndStudyId(String project, String studyId) {
    LOG.info("Get releases by " + project + " and " + studyId);
    StudyDatasetInfo datasetInfo = checkPermsAndFetchDatasetInfo(studyId, StudyAccess::allowStudyMetadata);
    return GetDownloadByProjectAndStudyIdResponse.respond200WithApplicationJson(
        new FileStore(Resources.getDatasetsParentDir(project))
            .getReleaseNames(datasetInfo.getSha1Hash())
            .orElseThrow(NotFoundException::new));
  }

  @Override
  public GetDownloadByProjectAndStudyIdAndReleaseResponse getDownloadByProjectAndStudyIdAndRelease(String project, String studyId, String release) {
    LOG.info("Get files by " + project + " and " + studyId + " and " + release);
    StudyDatasetInfo datasetInfo = checkPermsAndFetchDatasetInfo(studyId, StudyAccess::allowStudyMetadata);
    return GetDownloadByProjectAndStudyIdAndReleaseResponse.respond200WithApplicationJson(
        new FileStore(Resources.getDatasetsParentDir(project))
            .getFiles(datasetInfo.getSha1Hash(), release)
            .orElseThrow(NotFoundException::new));
  }

  @Override
  public GetDownloadByProjectAndStudyIdAndReleaseAndFileResponse getDownloadByProjectAndStudyIdAndReleaseAndFile(String project, String studyId, String release, String fileName) {
    LOG.info("Get file content at " + project + " and " + studyId + " and " + release + " and " + fileName);
    StudyDatasetInfo datasetInfo = checkPermsAndFetchDatasetInfo(studyId, StudyAccess::allowResultsAll);
    Path filePath = new FileStore(Resources.getDatasetsParentDir(project))
        .getFilePath(datasetInfo.getSha1Hash(), release, fileName)
        .orElseThrow(() -> {
          LOG.info("Unable to find a file with hash {}, release {}, and name {}.", datasetInfo.getSha1Hash(), release, fileName);
          return new NotFoundException();
        });
    String dispositionHeaderValue = "attachment; filename=\"" + fileName + "\"";
    Optional<String> userId = UserProvider.lookupUser(_request)
        .map(user -> Long.toString(user.getUserID()));
    ServiceMetrics.reportDownloadCount(datasetInfo.getDatasetId(), userId.orElse("None"), fileName);
    _request.setProperty(CustomResponseHeadersFilter.CUSTOM_HEADERS_KEY,
        new MapBuilder<>(HttpHeaders.CONTENT_DISPOSITION, dispositionHeaderValue).toMap());
    return GetDownloadByProjectAndStudyIdAndReleaseAndFileResponse.respond200WithTextPlain(
        new FileContentResponseStream(cSwallow(
            out -> IoUtil.transferStream(out, Files.newInputStream(filePath)))));
  }

  private StudyDatasetInfo checkPermsAndFetchDatasetInfo(String studyId, Function<StudyAccess, Boolean> accessGranter) {
    try {
      Entry<String, String> authHeader = UserProvider.getSubmittedAuth(_request).orElseThrow();
      Map<String, StudyDatasetInfo> studyMap =
          new DatasetAccessClient(Resources.DATASET_ACCESS_SERVICE_URL, authHeader).getStudyDatasetInfoMapForUser();
      StudyDatasetInfo study = studyMap.get(studyId);
      if (study == null) {
        throw new NotFoundException("Study '" + studyId + "' cannot be found [dataset access service].");
      }
      if (!accessGranter.apply(study.getStudyAccess())) {
        throw new ForbiddenException("Permission Denied");
      }
      return study;
    } catch (Exception e) {
      LOG.error("Unable to check study permissions and convert studyId to dataset hash", e);
      throw e;
    }
  }
}
