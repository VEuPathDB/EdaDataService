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

import static org.veupathdb.service.eda.ds.service.AppsService.processRequest;
import static org.veupathdb.service.eda.ds.service.AppsService.wrapPlugin;

@Authenticated(allowGuests = true)
public class FilterAwareMetadataService implements FilterAwareMetadataContinuousVariable {

  private static final Logger LOG = LogManager.getLogger(FilterAwareMetadataService.class);

  @Context
  private ContainerRequest _request;

  @DisableJackson
  @Override
  public PostFilterAwareMetadataContinuousVariableResponse postFilterAwareMetadataContinuousVariable(ContinuousVariableMetadataPostRequest entity) {
    return wrapPlugin(() -> PostFilterAwareMetadataContinuousVariableResponse.respond200WithApplicationJson(
        new ContinuousVariableMetadataPostResponseStream(processRequest(new ContinuousVariablePlugin(), entity, _request))));
  }

}
