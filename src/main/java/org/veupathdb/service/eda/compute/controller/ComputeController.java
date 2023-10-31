package org.veupathdb.service.eda.compute.controller;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;
import org.gusdb.fgputil.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.veupathdb.lib.container.jaxrs.providers.UserProvider;
import org.veupathdb.lib.container.jaxrs.server.annotations.Authenticated;
import org.veupathdb.service.eda.Main;
import org.veupathdb.service.eda.common.client.EdaMergingClient;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.compute.EDA;
import org.veupathdb.service.eda.compute.jobs.ReservedFiles;
import org.veupathdb.service.eda.compute.plugins.PluginMeta;
import org.veupathdb.service.eda.compute.plugins.PluginProvider;
import org.veupathdb.service.eda.compute.plugins.PluginRegistry;
import org.veupathdb.service.eda.compute.plugins.alphadiv.AlphaDivPluginProvider;
import org.veupathdb.service.eda.compute.plugins.betadiv.BetaDivPluginProvider;
import org.veupathdb.service.eda.compute.plugins.differentialabundance.DifferentialAbundancePluginProvider;
import org.veupathdb.service.eda.compute.plugins.example.ExamplePluginProvider;
import org.veupathdb.service.eda.compute.plugins.rankedabundance.RankedAbundancePluginProvider;
import org.veupathdb.service.eda.generated.model.AlphaDivPluginRequest;
import org.veupathdb.service.eda.generated.model.BetaDivPluginRequest;
import org.veupathdb.service.eda.generated.model.ComputeRequestBase;
import org.veupathdb.service.eda.generated.model.DifferentialAbundancePluginRequest;
import org.veupathdb.service.eda.generated.model.DifferentialAbundanceStatsResponseStream;
import org.veupathdb.service.eda.generated.model.ExamplePluginRequest;
import org.veupathdb.service.eda.generated.model.JobResponse;
import org.veupathdb.service.eda.generated.model.RankedAbundancePluginRequest;
import org.veupathdb.service.eda.generated.resources.Computes;
import org.veupathdb.service.eda.generated.support.ResponseDelegate;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Compute Plugins Controller
 * <p>
 * This controller is the home location where specific compute plugin endpoints
 * are registered.
 * <p>
 * New plugin endpoints should be added in the Plugin Endpoints region in this
 * file, after the "Plugin Endpoints" doc block and before the "endregion Plugin
 * Endpoints" statement.
 * <p>
 * Plugin endpoints should follow the example set by the
 * {@link #postComputesExample(Boolean, ExamplePluginRequest)} method and call the
 * {@link #submitJob(PluginProvider, ComputeRequestBase, boolean)} method, passing in an
 * instance of the target {@link PluginProvider} for their plugin along with the
 * raw request body (entity).
 *
 * @author Elizabeth Paige Harper - https://github.com/foxcapades
 * @since 1.0.0
 */
@Authenticated(allowGuests = true)
public class ComputeController implements Computes {

  private static final Logger LOG = LogManager.getLogger(ComputeController.class);

  private static final String TABULAR = "tabular";
  private static final String METADATA = "meta";
  private static final String STATISTICS = "statistics";

  @Context
  private ContainerRequest request;

  // region Plugin Endpoints
  // ╔════════════════════════════════════════════════════════════════════╗ //
  // ║  Plugin Endpoints                                                  ║ //
  // ║                                                                    ║ //
  // ║  Controller methods for handling requests to run specific          ║ //
  // ║  plugins.                                                          ║ //
  // ╚════════════════════════════════════════════════════════════════════╝ //


  @Override
  public PostComputesExampleResponse postComputesExample(Boolean autostart, ExamplePluginRequest entity) {
    return PostComputesExampleResponse.respond200WithApplicationJson(submitJob(new ExamplePluginProvider(), entity, autostart));
  }

  @Override
  public PostComputesExampleByFileResponse postComputesExampleByFile(String file, ExamplePluginRequest entity) {
    return resultFile(new ExamplePluginProvider(), file, entity, PostComputesExampleByFileResponse::respond200WithTextPlain);
  }

  @Override
  public PostComputesBetadivResponse postComputesBetadiv(Boolean autostart, BetaDivPluginRequest entity) {
    return PostComputesBetadivResponse.respond200WithApplicationJson(submitJob(new BetaDivPluginProvider(), entity, autostart));
  }

  @Override
  public PostComputesBetadivByFileResponse postComputesBetadivByFile(String file, BetaDivPluginRequest entity) {
    return resultFile(new BetaDivPluginProvider(), file, entity, PostComputesBetadivByFileResponse::respond200WithTextPlain);
  }

  @Override
  public PostComputesAlphadivResponse postComputesAlphadiv(Boolean autostart, AlphaDivPluginRequest entity) {
    return PostComputesAlphadivResponse.respond200WithApplicationJson(submitJob(new AlphaDivPluginProvider(), entity, autostart));
  }

  @Override
  public PostComputesAlphadivByFileResponse postComputesAlphadivByFile(String file, AlphaDivPluginRequest entity) {
    return resultFile(new AlphaDivPluginProvider(), file, entity, PostComputesAlphadivByFileResponse::respond200WithTextPlain);
  }

  @Override
  public PostComputesRankedabundanceResponse postComputesRankedabundance(Boolean autostart, RankedAbundancePluginRequest entity) {
    return PostComputesRankedabundanceResponse.respond200WithApplicationJson(submitJob(new RankedAbundancePluginProvider(), entity, autostart));
  }

  @Override
  public PostComputesRankedabundanceByFileResponse postComputesRankedabundanceByFile(String file, RankedAbundancePluginRequest entity) {
    return resultFile(new RankedAbundancePluginProvider(), file, entity, PostComputesRankedabundanceByFileResponse::respond200WithTextPlain);
  }

  @Override
  public PostComputesDifferentialabundanceResponse postComputesDifferentialabundance(Boolean autostart, DifferentialAbundancePluginRequest entity) {
    return PostComputesDifferentialabundanceResponse.respond200WithApplicationJson(submitJob(new DifferentialAbundancePluginProvider(), entity, autostart));
  }

  @Override
  public PostComputesDifferentialabundanceStatisticsResponse postComputesDifferentialabundanceStatistics(DifferentialAbundancePluginRequest entity) {
    return PostComputesDifferentialabundanceStatisticsResponse.respond200WithApplicationJson(new DifferentialAbundanceStatsResponseStream(out -> {
        try {
          getResultFileStreamer(new DifferentialAbundancePluginProvider(), STATISTICS, entity).write(out);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }));
  }


  // endregion Plugin Endpoints

  // region Constant Endpoints
  // ╔════════════════════════════════════════════════════════════════════╗ //
  // ║  Constant Endpoints                                                ║ //
  // ║                                                                    ║ //
  // ║  Endpoints that must exist regardless of what plugins are added    ║ //
  // ║  or removed.                                                       ║ //
  // ╚════════════════════════════════════════════════════════════════════╝ //

  @Override
  public GetComputesResponse getComputes() {
    return GetComputesResponse.respond200WithApplicationJson(PluginRegistry.getPluginOverview());
  }

  // endregion Constant Endpoints

  // ╔════════════════════════════════════════════════════════════════════╗ //
  // ║  Helper Methods                                                    ║ //
  // ╚════════════════════════════════════════════════════════════════════╝ //

  /**
   * Submits a new plugin execution job to one of the internal job queues.
   *
   * @param plugin {@code PluginProvider} that will be used to validate and
   * submit the new job request.
   *
   * @param requestObject The raw request payload.
   *
   * @param autostart Whether to start a job if none already exists
   *
   * @return Basic information about the submitted job to be returned to the
   * caller.
   *
   * @param <R> Type of the raw request body that the target plugin accepts.
   *
   * @param <C> Type of the configuration wrapped by the raw request body that
   * the target plugin accepts.
   */
  private <R extends ComputeRequestBase, C> JobResponse submitJob(PluginProvider<R, C> plugin, R requestObject, boolean autostart) {
    var auth = UserProvider.getSubmittedAuth(request).orElseThrow();

    requirePermissions(requestObject, auth);

    // Validate the request body
    Supplier<ReferenceMetadata> referenceMetadata = () -> {
      var studyId = requestObject.getStudyId();
      var meta = new ReferenceMetadata(
          EDA.getAPIStudyDetail(studyId, auth)
              .orElseThrow(() -> new BadRequestException("Invalid study ID: " + studyId)));
      var derivedVars = Optional.ofNullable(requestObject.getDerivedVariables()).orElse(Collections.emptyList());
      if (!derivedVars.isEmpty()) {
        var mergeClient = new EdaMergingClient(Main.config.getEdaMergeHost(), auth);
        for (var derivedVar : mergeClient.getDerivedVariableMetadata(studyId, derivedVars)) {
          meta.incorporateDerivedVariable(derivedVar);
        }
      }
      return meta;
    };

    // make sure config property was submitted with non-null value
    try {
      Object config = requestObject.getClass().getMethod("getConfig").invoke(requestObject);
      if (config == null)
        throw new BadRequestException("The request object does not contain a 'config' property value.");
    }
    catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("Request object type does not contain a callable 'getConfig()' method");
    }

    plugin.getValidator()
      .validate(requestObject, referenceMetadata);

    return EDA.getOrSubmitComputeJob(plugin, requestObject, auth, autostart);
  }

  // generic wrapper around the streaming output producer below
  public <R extends ResponseDelegate, P extends ComputeRequestBase> R resultFile(
    PluginMeta<P> plugin,
    String file,
    ComputeRequestBase entity,
    Function<Object, R> responseFn
  ) {
    return responseFn.apply(getResultFileStreamer(plugin, file, entity));
  }

  /**
   * Returns a stream over the contents of the target {@code file} from the job
   * described by the given configuration {@code entity}.
   *
   * @param plugin Metadata about the plugin to which the target job belongs.
   *
   * @param file Name of the filetype target. One of "meta", "tabular", or "statistics".
   *
   * @param entity Job configuration request body.
   *
   * @return The controller response type generated by the given {@code responseFn} function.
   *
   * @param <P> Type of the job configuration request body.
   */
  private <P extends ComputeRequestBase> StreamingOutput getResultFileStreamer(
      PluginMeta<P> plugin,
      String file,
      ComputeRequestBase entity
  ) {
    // If there was no plugin with the given name, throw a 404
    if (plugin == null)
      throw new NotFoundException();

    requirePermissions(entity, null);

    var jobFiles = EDA.getComputeJobFiles(plugin, entity);

    var fileName = switch(file) {
      case METADATA   -> ReservedFiles.OutputMeta;
      case TABULAR    -> ReservedFiles.OutputTabular;
      case STATISTICS -> ReservedFiles.OutputStats;
      default         -> throw new NotFoundException();
    };

    var fileRef = jobFiles.stream()
      .filter(f -> f.getName().equals(fileName))
      .findFirst()
      .orElseThrow(NotFoundException::new);

    return output -> {
      try (var input = fileRef.open()) {
        input.transferTo(output);
      }
      catch (IOException e) {
        throw new RuntimeException("Unable to stream result file", e);
      }
    };
  }

  /**
   * Ensures that the requester has the required permission(s) to call a plugin
   * endpoint for a target study.
   * <p>
   * As of the time of this writing, the permission required for plugin
   * execution and lookup is "allowVisualizations".
   *
   * @param entity Raw request body containing the ID of the study the user must
   * have permissions on.
   *
   * @param auth The auth header to use in validation, or {@code null} for the
   * auth header from the {@link #request} context to be used.
   *
   * @throws ForbiddenException If the requester does not have the required
   * permission(s) on the target study.
   */
  private void requirePermissions(@NotNull ComputeRequestBase entity, @Nullable Tuples.TwoTuple<String, String> auth) {
    if (auth == null)
      auth = UserProvider.getSubmittedAuth(request).orElseThrow();

    // Check that the user has permission to run compute jobs.
    if (!EDA.getStudyPerms(entity.getStudyId(), auth).allowVisualizations())
      throw new ForbiddenException();
  }
}
