package org.veupathdb.service.eda.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import org.glassfish.jersey.server.ContainerRequest;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.json.JsonUtil;
import org.json.JSONObject;
import org.veupathdb.lib.container.jaxrs.model.User;
import org.veupathdb.lib.container.jaxrs.providers.UserProvider;
import org.veupathdb.service.eda.Resources;
import org.veupathdb.service.eda.common.client.DatasetAccessClient;
import org.veupathdb.service.eda.user.model.AnalysisDetailWithUser;
import org.veupathdb.service.eda.user.model.UserDataFactory;

import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Utils {

  public static final ObjectMapper JSON = new ObjectMapper();

  public static User getActiveUser(ContainerRequest request) {
    return UserProvider.lookupUser(request).orElseThrow(() ->
        // authentication framework should handle cases where no appropriate user header was sent
        new InternalServerErrorException("Request reached authenticated endpoint with no user attached"));
  }

  public static User getAuthorizedUser(ContainerRequest request, String resourceUserId) {
    User activeUser = getActiveUser(request);
    if (!String.valueOf(activeUser.getUserID()).equals(resourceUserId)) {
      throw new ForbiddenException();
    }
    return activeUser;
  }

  public static String getCurrentDateTimeString() {
    return FormatUtil.formatDateTimeNoTimezone(new Date());
  }

  public static String formatTimestamp(Timestamp timestamp) {
    return FormatUtil.formatDateTimeNoTimezone(new Date(timestamp.getTime()));
  }

  public static Date parseDate(String dateString) {
    try {
      return FormatUtil.STANDARD_DATE_TIME_TEXT_FORMAT.get().parse(dateString);
    }
    catch (ParseException e) {
      throw new BadRequestException();
    }
  }

  public static void verifyOwnership(UserDataFactory dataFactory, long userId, String... ids) {
    List<String> usersAnalysisIds = dataFactory.getAnalysisSummaries(userId).stream()
        .map(sum -> sum.getAnalysisId()).collect(Collectors.toList());
    List<String> badIds = Arrays.stream(ids)
        .filter(id -> !usersAnalysisIds.contains(id)).collect(Collectors.toList());
    if (!badIds.isEmpty()) {
      throw new NotFoundException("Analysis ID(s) [ " + String.join(", ", badIds) + " ] cannot be found for user " + userId);
    }
  }

  public static void verifyOwnership(long userId, AnalysisDetailWithUser analysis) {
    if (userId != analysis.getUserId()) {
      throw new NotFoundException();
    }
  }

  public static <T> T parseObject(String jsonString, Class<T> clazz) {
    try {
      return jsonString == null ? null : JsonUtil.Jackson.readValue(jsonString, clazz);
    }
    catch (JsonProcessingException e) {
      throw new RuntimeException("Could not map JSON string to a " + clazz.getName() + " object", e);
    }
  }

  public static String issueUUID() {
    return UUID.randomUUID().toString();
  }

  public static String formatObject(Object obj) {
    try {
      return JsonUtil.Jackson.writeValueAsString(obj);
    }
    catch (JsonProcessingException e) {
      throw new RuntimeException("Could not serialize " + obj.getClass().getName(), e);
    }
  }

  public static String checkNonEmpty(String key, String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new BadRequestException(key + " must not be empty.");
    }
    return value.trim();
  }

  public static String checkMaxSize(int maxSize, String key, String value) {
    if (value != null && FormatUtil.getUtf8EncodedBytes(value).length > maxSize) {
      throw new BadRequestException(key + " must not be larger than " + maxSize + " bytes.");
    }
    return value;
  }

  public static boolean isNullOrBlank(String value) {
    return value == null || value.isBlank();
  }

  public static boolean isNullOrEmpty(Collection<?> values) {
    return values == null || values.isEmpty();
  }

  public static <T, R> R mapIfPresent(T value, Function<T, R> mapper) {
    return value == null ? null : mapper.apply(value);
  }

  public static <T> void callIfPresent(T value, Consumer<T> fn) {
    if (value != null)
      fn.accept(value);
  }

  public static <T> T orElse(T value, T fallback) {
    return value == null ? fallback : value;
  }

  public static void requireSubsettingPermission(ContainerRequest request, String datasetID) {
    try {
      var info = new DatasetAccessClient(
        Resources.DATASET_ACCESS_SERVICE_URL,
        UserProvider.getSubmittedAuth(request).orElseThrow()
      )
        .getStudyPermsByDatasetId(datasetID);

      if (!info.getStudyAccess().allowSubsetting()) {
        throw new ForbiddenException(new JSONObject()
          .put("denialReason", "noAccess")
          .put("message", "The requesting user does not have access to this study.")
          .put("datasetId", datasetID)
          .put("isUserDataset", info.isUserStudy())
          .toString()
        );
      }
    } catch (NotFoundException e) {
      // per https://github.com/VEuPathDB/EdaUserService/issues/24 if dataset under the study does not exist, throw Forbidden
      throw new ForbiddenException(new JSONObject()
        .put("denialReason", "missingDataset")
        .put("message", "This analysis cannot be imported because the underlying dataset '" + datasetID + "' no longer exists.")
        .put("datasetId", datasetID)
        .toString());
    }
  }
}
