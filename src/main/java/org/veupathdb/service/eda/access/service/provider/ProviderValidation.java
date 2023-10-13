package org.veupathdb.service.eda.access.service.provider;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import org.apache.logging.log4j.Logger;
import org.veupathdb.lib.container.jaxrs.errors.UnprocessableEntityException;
import org.veupathdb.lib.container.jaxrs.providers.LogProvider;
import org.veupathdb.service.eda.generated.model.DatasetProviderCreateRequest;
import org.veupathdb.service.eda.access.service.account.AccountRepo;
import org.veupathdb.service.eda.access.service.dataset.DatasetRepo;
import org.veupathdb.service.eda.access.util.Keys;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProviderValidation
{
  private static final String
    ERR_REQUIRED = "Value is required.",
    ERR_EMAIL_ID = "Request must include a non-blank value for exactly one of the fields \""
      + Keys.Json.KEY_USER_ID + "\" or \"" + Keys.Json.KEY_EMAIL + "\".",
    ERR_NO_USER  = "Specified user does not exist.",
    ERR_NO_DS    = "Specified dataset does not exist.";

  private static ProviderValidation instance;

  private final Logger log;

  private ProviderValidation() {
    log = LogProvider.logger(getClass());
  }

  public enum CreateState {
    /**
     * Indicates that the request validates against an existing user and the
     * creation of the provider record may proceed.
     */
    CREATE_PROVIDER,

    /**
     * Indicates that the request is valid, but the user does not exist so the
     * provider record cannot be created.  In this case the user should be
     * emailed instead with a registration link.
     */
    SEND_EMAIL
  }

  /**
   * Validate Create Request Body
   * <p>
   * Verifies the following:
   * <ul>
   *   <li>Dataset ID value is set</li>
   *   <li>Dataset ID value is valid (points to a real dataset)</li>
   *   <li>
   *     Exactly one of the following is true:
   *     <ul>
   *       <li>User ID is set and is valid</li>
   *       <li>User email is set</li>
   *     </ul>
   *   </li>
   *   <li>No provider currently exists for this user/dataset already.</li>
   * </ul>
   *
   * @param body Provider create request body.
   *
   * @return A status indicator informing the caller which form of request the
   * input validated against: A valid request for a user that does not yet exist
   * or a valid request for an already existing user.
   *
   * @throws ForbiddenException           if a provider already exists for the given user
   *                                      & dataset id values.
   * @throws UnprocessableEntityException if the dataset id value is not set or
   *                                      invalid, if the user id is not valid.
   * @throws InternalServerErrorException if a database error occurs while
   *                                      attempting to validate this request.
   */
  public CreateState validateCreateRequest(final DatasetProviderCreateRequest body) {
    log.trace("ProviderService#validateCreate(DatasetProviderCreateRequest)");

    final var out = new HashMap<String, List<String>>();

    try {
      // Verify that the given dataset id actually exists.
      lookupDataset(body.getDatasetId(), out);

      // Attempt to lookup the user, returns a state indicator that informs
      // whether the user was found or not.
      final var st = fillUser(body, out);

      // If the above methods appended errors to the
      if (!out.isEmpty())
        throw new UnprocessableEntityException(out);

      if (st == FILL_STATE_DO_EMAIL)
        return CreateState.SEND_EMAIL;

      if (ProviderRepo.Select.byUserAndDataset(body.getUserId(), body.getDatasetId()).isPresent())
        throw new ForbiddenException("A provider record already exists for this user and dataset.");

      return CreateState.CREATE_PROVIDER;
    } catch (WebApplicationException e) {
      throw e;
    } catch (Throwable e) {
      throw new InternalServerErrorException(e);
    }
  }

  void lookupDataset(final String dsId, final Map<String, List<String>> errs)
  throws Exception {
    if (dsId == null || dsId.isBlank())
      errs.put(Keys.Json.KEY_DATASET_ID, Collections.singletonList(ERR_REQUIRED));

    if (!DatasetRepo.Select.datasetExists(dsId))
      errs.put(Keys.Json.KEY_DATASET_ID, Collections.singletonList(ERR_NO_DS));
  }

  private static final int
    FILL_STATE_HAS_USER = 0,
    FILL_STATE_DO_EMAIL = 1,
    FILL_STATE_FAILED   = 2;

  int fillUser(final DatasetProviderCreateRequest body, final Map<String, List<String>> errs)
  throws Exception {
    // Client provided a user id
    if (body.getUserId() != null && body.getUserId() > 0) {

      // Invalid user id
      if (!AccountRepo.Select.userExists(body.getUserId())) {
        errs.put(Keys.Json.KEY_USER_ID, Collections.singletonList(ERR_NO_USER));
        return FILL_STATE_FAILED;
      }

      // Valid user id
      return FILL_STATE_HAS_USER;
    }

    // has neither a user id nor an email address
    if (body.getEmail() == null || body.getEmail().isBlank()) {
      var err = Collections.singletonList(ERR_EMAIL_ID);
      errs.put(Keys.Json.KEY_USER_ID, err);
      errs.put(Keys.Json.KEY_EMAIL, err);

      return FILL_STATE_FAILED;
    }

    // lookup user id by email
    var opt = AccountRepo.Select.getInstance().selectUserIdByEmail(body.getEmail());
    opt.ifPresent(body::setUserId);

    return opt.isPresent() ? FILL_STATE_HAS_USER : FILL_STATE_DO_EMAIL;
  }

  public static ProviderValidation getInstance() {
    if (instance == null)
      instance = new ProviderValidation();

    return instance;
  }
}
