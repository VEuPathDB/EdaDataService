package org.veupathdb.service.eda.compute.controller;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.veupathdb.lib.compute.platform.AsyncPlatform;
import org.veupathdb.lib.compute.platform.job.JobFileReference;
import org.veupathdb.lib.compute.platform.job.JobStatus;
import org.veupathdb.lib.compute.platform.model.JobReference;
import org.veupathdb.lib.hash_id.HashID;
import org.veupathdb.service.eda.compute.jobs.ReservedFiles;
import org.veupathdb.service.eda.compute.service.ServiceOptions;
import org.veupathdb.service.eda.generated.model.ExpiredJobsResponse;
import org.veupathdb.service.eda.generated.model.ExpiredJobsResponseImpl;
import org.veupathdb.service.eda.generated.resources.ExpireComputeJobs;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

/**
 * Responsible for expiring jobs on command.  Service takes two optional arguments:
 *
 * - study ID: if passed, endpoint will only expire jobs run against this study
 * - plugin name: if passed, endpoint will only expire jobs run using this plugin
 *
 * If neither argument is passed, a call to this endpoint will expire all job results
 */
public class ExpirationController implements ExpireComputeJobs {

  private static final Logger LOG = LogManager.getLogger(ExpirationController.class);

  @Override
  public GetExpireComputeJobsResponse getExpireComputeJobs(String jobId, String studyId, String pluginName, String adminAuthToken) {
    if (adminAuthToken == null || !adminAuthToken.equals(ServiceOptions.getAdminAuthToken())) {
      throw new ForbiddenException();
    }
    if (jobId != null && (studyId != null || pluginName != null)) {
      throw new BadRequestException("If job-id param is specified, study-id and plugin-name are not allowed.");
    }
    List<HashID> filteredJobIds = findJobs(Optional.ofNullable(jobId), Optional.ofNullable(studyId), Optional.ofNullable(pluginName));
    int numJobsExpired = manuallyExpireJobs(filteredJobIds);
    LOG.info("Expired " + numJobsExpired + " jobs in response to the following expiration request: " +
        "jobId = " + jobId + ", studyId = " + studyId + ", pluginName = " + pluginName);
    ExpiredJobsResponse response = new ExpiredJobsResponseImpl();
    response.setNumJobsExpired(numJobsExpired);
    return GetExpireComputeJobsResponse.respond200WithApplicationJson(response);
  }

  private List<HashID> findJobs(Optional<String> jobIdOption, Optional<String> studyIdOption, Optional<String> pluginNameOption) {
    ForkJoinPool customThreadPool = null;
    try {
      customThreadPool = new ForkJoinPool(10);
      return customThreadPool.submit(() ->

        AsyncPlatform.listJobReferences().parallelStream()

        // can only expire owned jobs
        .filter(JobReference::getOwned)

        // convert to job ID
        .map(JobReference::getJobID)

        // filter out already-expired jobs
        .filter(jobId -> !JobStatus.Expired.equals(AsyncPlatform.getJob(jobId).getStatus()))

        // filter jobs by requested criteria
        .filter(jobId ->

          // if job ID specified, then match exactly if job IDs match
          jobIdOption.isPresent() ? jobId.getString().equals(jobIdOption.get()) :

          // only need to look up config if criteria specified
          (studyIdOption.isEmpty() && pluginNameOption.isEmpty()) ||
              jobMatchesCriteria(jobId, studyIdOption, pluginNameOption)
        )

        // collect remaining job IDs
        .toList()
      ).get();
    }
    catch (ExecutionException e) {
      throw new RuntimeException("Could not handle job expiration request", e);
    }
    catch (InterruptedException e) {
      throw new RuntimeException("Expiration request interrupted before completion", e);
    }
    finally {
      customThreadPool.shutdown();
    }
  }

  private boolean jobMatchesCriteria(HashID jobId, Optional<String> studyIdOption, Optional<String> pluginNameOption) {
    // find the config file
    JobFileReference configFile = AsyncPlatform.INSTANCE.getJobFile(jobId, ReservedFiles.InputConfig);
    if (configFile == null) {
      LOG.warn("Could not find job config file in non-expired job " + jobId);
      return false;
    }
    // read the config file
    try (InputStream in = configFile.open()) {
      JSONObject json = new JSONObject(new String(in.readAllBytes()));
      String pluginName = json.getString("plugin");
      String studyId = json.getJSONObject("request").getString("studyId");
      boolean matchesPluginName = pluginNameOption.map(pluginName::equals).orElse(true);
      boolean matchesStudyId = studyIdOption.map(studyId::equals).orElse(true);
      return matchesPluginName && matchesStudyId;
    }
    catch (IOException e) {
      LOG.error("Could not open or parse job config file for job " + jobId);
      return true;
    }
  }

  private int manuallyExpireJobs(List<HashID> filteredJobIds) {
    int count = 0;
    for (HashID id : filteredJobIds) {
      try {
        AsyncPlatform.expireJob(id);
        count++;
      }
      catch (Exception e) {
        LOG.error("Unable to expire Job with ID " + id, e);
      }
    }
    return count;
  }

}
