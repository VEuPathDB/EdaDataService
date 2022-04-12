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
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.ds.plugin.abundance.AbundanceBoxplotPlugin;
import org.veupathdb.service.eda.ds.plugin.abundance.AbundanceScatterplotPlugin;
import org.veupathdb.service.eda.ds.plugin.alphadiv.AlphaDivBoxplotPlugin;
import org.veupathdb.service.eda.ds.plugin.alphadiv.AlphaDivScatterplotPlugin;
import org.veupathdb.service.eda.ds.plugin.betadiv.BetaDivScatterplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.BarplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.BoxplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.ContTablePlugin;
import org.veupathdb.service.eda.ds.plugin.pass.DensityplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.HeatmapPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.HistogramPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.LineplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.MapPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.PieplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.ScatterplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.TablePlugin;
import org.veupathdb.service.eda.ds.plugin.pass.TimeSeriesPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.TwoByTwoPlugin;
import org.veupathdb.service.eda.ds.plugin.sample.MultiStreamPlugin;
import org.veupathdb.service.eda.ds.plugin.sample.RecordCountPlugin;
import org.veupathdb.service.eda.ds.plugin.sample.TestCollectionPlugin;
import org.veupathdb.service.eda.ds.util.NonEmptyResultStream.EmptyResultException;
import org.veupathdb.service.eda.generated.model.*;
import org.veupathdb.service.eda.generated.resources.Apps;

@Authenticated(allowGuests = true)
public class AppsService implements Apps {

  private static final Logger LOG = LogManager.getLogger(AppsService.class);

  @Context
  private ContainerRequest _request;

  @Override
  public GetAppsResponse getApps() {
    return GetAppsResponse.respond200WithApplicationJson(AppsMetadata.APPS);
  }

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
      LOG.error("Could not execute app.", e);
      throw new ServerErrorException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  private <T extends VisualizationRequestBase> Consumer<OutputStream> processRequest(AbstractPlugin<T,?> plugin, T entity) throws ValidationException {
    Entry<String,String> authHeader = UserProvider.getSubmittedAuth(_request).orElseThrow();
    StudyAccess.confirmPermission(authHeader, Resources.DATASET_ACCESS_SERVICE_URL,
        entity.getStudyId(), StudyAccess::allowVisualizations);
    String appName = _request.getUriInfo().getPathSegments().get(1).getPath();
    return plugin.processRequest(appName, entity, authHeader);
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsMapMarkersResponse postAppsPassVisualizationsMapMarkers(MapPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsMapMarkersResponse.respond200WithApplicationJson(
        new MapPostResponseStream(processRequest(new MapPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsTableResponse postAppsPassVisualizationsTable(TablePostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsTableResponse.respond200WithApplicationJson(
        new TablePostResponseStream(processRequest(new TablePlugin(), entity))));
  }
  
  @DisableJackson
  @Override
  public PostAppsPassVisualizationsScatterplotResponse postAppsPassVisualizationsScatterplot(ScatterplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsScatterplotResponse.respond200WithApplicationJson(
        new ScatterplotPostResponseStream(processRequest(new ScatterplotPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsDensityplotResponse postAppsPassVisualizationsDensityplot(DensityplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsDensityplotResponse.respond200WithApplicationJson(
        new DensityplotPostResponseStream(processRequest(new DensityplotPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsLineplotResponse postAppsPassVisualizationsLineplot(LineplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsLineplotResponse.respond200WithApplicationJson(
        new LineplotPostResponseStream(processRequest(new LineplotPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsTimeseriesResponse postAppsPassVisualizationsTimeseries(LineplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsTimeseriesResponse.respond200WithApplicationJson(
        new LineplotPostResponseStream(processRequest(new TimeSeriesPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsHistogramResponse postAppsPassVisualizationsHistogram(HistogramPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsHistogramResponse.respond200WithApplicationJson(
        new HistogramPostResponseStream(processRequest(new HistogramPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsBarplotResponse postAppsPassVisualizationsBarplot(BarplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsBarplotResponse.respond200WithApplicationJson(
        new BarplotPostResponseStream(processRequest(new BarplotPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsPieplotResponse postAppsPassVisualizationsPieplot(PieplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsPieplotResponse.respond200WithApplicationJson(
        new PieplotPostResponseStream(processRequest(new PieplotPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsBoxplotResponse postAppsPassVisualizationsBoxplot(BoxplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsBoxplotResponse.respond200WithApplicationJson(
        new BoxplotPostResponseStream(processRequest(new BoxplotPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsHeatmapResponse postAppsPassVisualizationsHeatmap(HeatmapPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsHeatmapResponse.respond200WithApplicationJson(
        new HeatmapPostResponseStream(processRequest(new HeatmapPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsTwobytwoResponse postAppsPassVisualizationsTwobytwo(MosaicPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsTwobytwoResponse.respond200WithApplicationJson(
        new TwoByTwoPostResponseStream(processRequest(new TwoByTwoPlugin(), entity))));
  }
  
  @DisableJackson
  @Override
  public PostAppsPassVisualizationsConttableResponse postAppsPassVisualizationsConttable(MosaicPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsConttableResponse.respond200WithApplicationJson(
        new ContTablePostResponseStream(processRequest(new ContTablePlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsSampleVisualizationsRecordCountResponse postAppsSampleVisualizationsRecordCount(RecordCountPostRequest entity) {
    return wrapPlugin(() -> PostAppsSampleVisualizationsRecordCountResponse.respond200WithApplicationJson(
        new RecordCountPostResponseStream(processRequest(new RecordCountPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsSampleVisualizationsMultiStreamResponse postAppsSampleVisualizationsMultiStream(MultiStreamPostRequest entity) {
    return wrapPlugin(() -> PostAppsSampleVisualizationsMultiStreamResponse.respond200WithTextPlain(
        new EntityTabularPostResponseStream(processRequest(new MultiStreamPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsSampleVisualizationsTestCollectionsResponse postAppsSampleVisualizationsTestCollections(TestCollectionsPostRequest entity) {
    return wrapPlugin(() -> PostAppsSampleVisualizationsTestCollectionsResponse.respond200WithTextPlain(
        new EntityTabularPostResponseStream(processRequest(new TestCollectionPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsAlphadivVisualizationsBoxplotResponse postAppsAlphadivVisualizationsBoxplot(AlphaDivBoxplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsAlphadivVisualizationsBoxplotResponse.respond200WithApplicationJson(
        new BoxplotPostResponseStream(processRequest(new AlphaDivBoxplotPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsAlphadivVisualizationsScatterplotResponse postAppsAlphadivVisualizationsScatterplot(AlphaDivScatterplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsAlphadivVisualizationsScatterplotResponse.respond200WithApplicationJson(
        new ScatterplotPostResponseStream(processRequest(new AlphaDivScatterplotPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsBetadivVisualizationsScatterplotResponse postAppsBetadivVisualizationsScatterplot(BetaDivScatterplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsBetadivVisualizationsScatterplotResponse.respond200WithApplicationJson(
        new ScatterplotPostResponseStream(processRequest(new BetaDivScatterplotPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsAbundanceVisualizationsBoxplotResponse postAppsAbundanceVisualizationsBoxplot(AbundanceBoxplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsAbundanceVisualizationsBoxplotResponse.respond200WithApplicationJson(
        new BoxplotPostResponseStream(processRequest(new AbundanceBoxplotPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsAbundanceVisualizationsScatterplotResponse postAppsAbundanceVisualizationsScatterplot(AbundanceScatterplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsAbundanceVisualizationsScatterplotResponse.respond200WithApplicationJson(
        new ScatterplotPostResponseStream(processRequest(new AbundanceScatterplotPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsDistributionsVisualizationsBoxplotResponse postAppsDistributionsVisualizationsBoxplot(BoxplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsDistributionsVisualizationsBoxplotResponse.respond200WithApplicationJson(
        new BoxplotPostResponseStream(processRequest(new BoxplotPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsDistributionsVisualizationsHistogramResponse postAppsDistributionsVisualizationsHistogram(HistogramPostRequest entity) {
    return wrapPlugin(() -> PostAppsDistributionsVisualizationsHistogramResponse.respond200WithApplicationJson(
        new HistogramPostResponseStream(processRequest(new HistogramPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsCountsandproportionsVisualizationsBarplotResponse postAppsCountsandproportionsVisualizationsBarplot(BarplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsCountsandproportionsVisualizationsBarplotResponse.respond200WithApplicationJson(
        new BarplotPostResponseStream(processRequest(new BarplotPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsCountsandproportionsVisualizationsTwobytwoResponse postAppsCountsandproportionsVisualizationsTwobytwo(MosaicPostRequest entity) {
    return wrapPlugin(() -> PostAppsCountsandproportionsVisualizationsTwobytwoResponse.respond200WithApplicationJson(
        new TwoByTwoPostResponseStream(processRequest(new TwoByTwoPlugin(), entity))));
  }
  
  @DisableJackson
  @Override
  public PostAppsCountsandproportionsVisualizationsConttableResponse postAppsCountsandproportionsVisualizationsConttable(MosaicPostRequest entity) {
    return wrapPlugin(() -> PostAppsCountsandproportionsVisualizationsConttableResponse.respond200WithApplicationJson(
        new ContTablePostResponseStream(processRequest(new ContTablePlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsXyrelationshipsVisualizationsLineplotResponse postAppsXyrelationshipsVisualizationsLineplot(LineplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsXyrelationshipsVisualizationsLineplotResponse.respond200WithApplicationJson(
        new LineplotPostResponseStream(processRequest(new LineplotPlugin(), entity))));
  }

  @DisableJackson
  @Override
  public PostAppsXyrelationshipsVisualizationsScatterplotResponse postAppsXyrelationshipsVisualizationsScatterplot(ScatterplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsXyrelationshipsVisualizationsScatterplotResponse.respond200WithApplicationJson(
        new ScatterplotPostResponseStream(processRequest(new ScatterplotPlugin(), entity))));
  }

}
