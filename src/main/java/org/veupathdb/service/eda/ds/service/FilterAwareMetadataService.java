package org.veupathdb.service.eda.ds.service;

import java.io.OutputStream;
import java.util.Map.Entry;
import java.util.function.Consumer;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;
import org.gusdb.fgputil.functional.FunctionalInterfaces.SupplierWithException;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.lib.container.jaxrs.providers.UserProvider;
import org.veupathdb.lib.container.jaxrs.server.annotations.Authenticated;
import org.veupathdb.lib.container.jaxrs.server.annotations.DisableJackson;
import org.veupathdb.service.eda.common.auth.StudyAccess;
import org.veupathdb.service.eda.ds.Resources;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.ds.plugin.filteredmetadata.ContinuousVariablePlugin;
import org.veupathdb.service.eda.common.client.NonEmptyResultStream.EmptyResultException;
import org.veupathdb.service.eda.generated.model.*;
import org.veupathdb.service.eda.generated.resources.FilterAwareMetadataContinuousVariable;

@Authenticated(allowGuests = true)
public class FilterAwareMetadataService implements FilterAwareMetadataContinuousVariable {

  private static final Logger LOG = LogManager.getLogger(FilterAwareMetadataService.class);

  @Context
  private ContainerRequest _request;

  private <T> T wrapPlugin(SupplierWithException<T> supplier) {
    try {
      return supplier.get();
    }
    catch (ValidationException e) {
      throw new BadRequestException(e.getMessage());
    }
    catch (EmptyResultException e) {
      throw new WebApplicationException(e.getMessage(), 204);
    }
    catch (WebApplicationException e) {
      throw e;
    }
    catch (Exception e) {
      LOG.error("Could not execute metadata request.", e);
      throw new ServerErrorException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  private <T extends VisualizationRequestBase> Consumer<OutputStream> processRequest(AbstractPlugin<T,?,?> plugin, T entity) throws ValidationException {
    Entry<String,String> authHeader = UserProvider.getSubmittedAuth(_request).orElseThrow();
    // idk what perms make sense really...
    //StudyAccess.confirmPermission(authHeader, Resources.DATASET_ACCESS_SERVICE_URL,
    //    entity.getStudyId(), StudyAccess::allowVisualizations);
    return plugin.processRequest(null, entity, authHeader);
  }

  @DisableJackson
  @Override
  public PostFilterAwareMetadataContinuousVariableResponse postFilterAwareMetadataContinuousVariable(ContinuousVariableMetadataPostRequest entity) {
    return wrapPlugin(() -> PostFilterAwareMetadataContinuousVariableResponse.respond200WithApplicationJson(
        new ContinuousVariableMetadataPostResponseStream(processRequest(new ContinuousVariablePlugin(), entity))));
  }

}
