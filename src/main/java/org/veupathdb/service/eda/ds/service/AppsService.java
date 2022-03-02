package org.veupathdb.service.eda.ds.service;

import java.io.OutputStream;
import java.util.Map.Entry;
import java.util.function.Consumer;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.functional.FunctionalInterfaces.SupplierWithException;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.lib.container.jaxrs.server.annotations.Authenticated;
import org.veupathdb.lib.container.jaxrs.server.annotations.DisableJackson;
import org.veupathdb.lib.container.jaxrs.utils.RequestKeys;
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
import org.veupathdb.service.eda.ds.plugin.pass.ScatterplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.TablePlugin;
import org.veupathdb.service.eda.ds.plugin.pass.TimeSeriesPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.TwoByTwoPlugin;
import org.veupathdb.service.eda.ds.plugin.sample.MultiStreamPlugin;
import org.veupathdb.service.eda.ds.plugin.sample.RecordCountPlugin;
import org.veupathdb.service.eda.ds.util.NonEmptyResultStream.EmptyResultException;
import org.veupathdb.service.eda.generated.model.AbundanceBoxplotPostRequest;
import org.veupathdb.service.eda.generated.model.AbundanceScatterplotPostRequest;
import org.veupathdb.service.eda.generated.model.AlphaDivBoxplotPostRequest;
import org.veupathdb.service.eda.generated.model.AlphaDivScatterplotPostRequest;
import org.veupathdb.service.eda.generated.model.BarplotPostRequest;
import org.veupathdb.service.eda.generated.model.BarplotPostResponseStream;
import org.veupathdb.service.eda.generated.model.BetaDivScatterplotPostRequest;
import org.veupathdb.service.eda.generated.model.BoxplotPostRequest;
import org.veupathdb.service.eda.generated.model.BoxplotPostResponseStream;
import org.veupathdb.service.eda.generated.model.ContTablePostResponseStream;
import org.veupathdb.service.eda.generated.model.DensityplotPostRequest;
import org.veupathdb.service.eda.generated.model.DensityplotPostResponseStream;
import org.veupathdb.service.eda.generated.model.EntityTabularPostResponseStream;
import org.veupathdb.service.eda.generated.model.HeatmapPostRequest;
import org.veupathdb.service.eda.generated.model.HeatmapPostResponseStream;
import org.veupathdb.service.eda.generated.model.HistogramPostRequest;
import org.veupathdb.service.eda.generated.model.HistogramPostResponseStream;
import org.veupathdb.service.eda.generated.model.LineplotPostRequest;
import org.veupathdb.service.eda.generated.model.LineplotPostResponseStream;
import org.veupathdb.service.eda.generated.model.MapPostRequest;
import org.veupathdb.service.eda.generated.model.MapPostResponseStream;
import org.veupathdb.service.eda.generated.model.MosaicPostRequest;
import org.veupathdb.service.eda.generated.model.MultiStreamPostRequest;
import org.veupathdb.service.eda.generated.model.RecordCountPostRequest;
import org.veupathdb.service.eda.generated.model.RecordCountPostResponseStream;
import org.veupathdb.service.eda.generated.model.ScatterplotPostRequest;
import org.veupathdb.service.eda.generated.model.ScatterplotPostResponseStream;
import org.veupathdb.service.eda.generated.model.TablePostRequest;
import org.veupathdb.service.eda.generated.model.TablePostResponseStream;
import org.veupathdb.service.eda.generated.model.TwoByTwoPostResponseStream;
import org.veupathdb.service.eda.generated.model.VisualizationRequestBase;
import org.veupathdb.service.eda.generated.resources.Apps;

@Authenticated(allowGuests = true)
public class AppsService implements Apps {

  private static final Logger LOG = LogManager.getLogger(AppsService.class);

  @Context
  private ContainerRequestContext _request;

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
    Entry<String,String> authHeader = StudyAccess.readAuthHeader(_request, RequestKeys.AUTH_HEADER);
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
