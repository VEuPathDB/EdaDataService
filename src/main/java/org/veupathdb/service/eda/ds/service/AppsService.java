package org.veupathdb.service.eda.ds.service;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.functional.FunctionalInterfaces;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.lib.container.jaxrs.server.annotations.DisableJackson;
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
import org.veupathdb.service.eda.ds.plugin.pass.TablePlugin;
import org.veupathdb.service.eda.ds.plugin.pass.TimeSeriesPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.TwoByTwoPlugin;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.plugin.sample.RecordCountPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.ScatterplotPlugin;
import org.veupathdb.service.eda.ds.plugin.sample.MultiStreamPlugin;
import org.veupathdb.service.eda.generated.model.AbundanceBoxplotPostRequest;
import org.veupathdb.service.eda.generated.model.AbundanceBoxplotPostResponseStream;
import org.veupathdb.service.eda.generated.model.AbundanceScatterplotPostRequest;
import org.veupathdb.service.eda.generated.model.AbundanceScatterplotResponseStream;
import org.veupathdb.service.eda.generated.model.AlphaDivBoxplotPostRequest;
import org.veupathdb.service.eda.generated.model.AlphaDivBoxplotPostResponseStream;
import org.veupathdb.service.eda.generated.model.AlphaDivScatterplotPostRequest;
import org.veupathdb.service.eda.generated.model.AlphaDivScatterplotResponseStream;
import org.veupathdb.service.eda.ds.util.NonEmptyResultStream;
import org.veupathdb.service.eda.ds.util.NonEmptyResultStream.EmptyResultException;
import org.veupathdb.service.eda.generated.model.BarplotPostRequest;
import org.veupathdb.service.eda.generated.model.BarplotPostResponseStream;
import org.veupathdb.service.eda.generated.model.BetaDivScatterplotPostRequest;
import org.veupathdb.service.eda.generated.model.BetaDivScatterplotResponseStream;
import org.veupathdb.service.eda.generated.model.BoxplotPostRequest;
import org.veupathdb.service.eda.generated.model.BoxplotPostResponseStream;
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
import org.veupathdb.service.eda.generated.model.TablePostRequest;
import org.veupathdb.service.eda.generated.model.TablePostResponseStream;
import org.veupathdb.service.eda.generated.model.TwoByTwoPostResponseStream;
import org.veupathdb.service.eda.generated.model.ContTablePostResponseStream;
import org.veupathdb.service.eda.generated.model.MultiStreamPostRequest;
import org.veupathdb.service.eda.generated.model.RecordCountPostRequest;
import org.veupathdb.service.eda.generated.model.RecordCountPostResponseStream;
import org.veupathdb.service.eda.generated.model.ScatterplotPostRequest;
import org.veupathdb.service.eda.generated.model.ScatterplotPostResponseStream;
import org.veupathdb.service.eda.generated.resources.Apps;

public class AppsService implements Apps {

  private static final Logger LOG = LogManager.getLogger(AppsService.class);

  @Override
  public GetAppsResponse getApps() {
    return GetAppsResponse.respond200WithApplicationJson(AppsMetadata.APPS);
  }

  private <T> T wrapPlugin(FunctionalInterfaces.SupplierWithException<T> supplier) {
    try {
      return supplier.get();
    }
    catch (ValidationException e) {
      throw new BadRequestException(e.getMessage());
    }
    catch (EmptyResultException e) {
      throw new WebApplicationException(e.getMessage(), 204);
    }
    catch (Exception e) {
      LOG.error("Could not execute app.", e);
      throw new ServerErrorException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsMapMarkersResponse postAppsPassVisualizationsMapMarkers(MapPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsMapMarkersResponse.respond200WithApplicationJson(
        new MapPostResponseStream(new MapPlugin().processRequest(entity))));
  }
  
  @DisableJackson
  @Override
  public PostAppsPassVisualizationsTableResponse postAppsPassVisualizationsTable(TablePostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsTableResponse.respond200WithApplicationJson(
        new TablePostResponseStream(new TablePlugin().processRequest(entity))));
  }
  
  @DisableJackson
  @Override
  public PostAppsPassVisualizationsScatterplotResponse postAppsPassVisualizationsScatterplot(ScatterplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsScatterplotResponse.respond200WithApplicationJson(
        new ScatterplotPostResponseStream(new ScatterplotPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsDensityplotResponse postAppsPassVisualizationsDensityplot(DensityplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsDensityplotResponse.respond200WithApplicationJson(
        new DensityplotPostResponseStream(new DensityplotPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsLineplotResponse postAppsPassVisualizationsLineplot(LineplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsLineplotResponse.respond200WithApplicationJson(
        new LineplotPostResponseStream(new LineplotPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsTimeseriesResponse postAppsPassVisualizationsTimeseries(LineplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsTimeseriesResponse.respond200WithApplicationJson(
        new LineplotPostResponseStream(new TimeSeriesPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsHistogramResponse postAppsPassVisualizationsHistogram(HistogramPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsHistogramResponse.respond200WithApplicationJson(
        new HistogramPostResponseStream(new HistogramPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsBarplotResponse postAppsPassVisualizationsBarplot(BarplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsBarplotResponse.respond200WithApplicationJson(
        new BarplotPostResponseStream(new BarplotPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsBoxplotResponse postAppsPassVisualizationsBoxplot(BoxplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsBoxplotResponse.respond200WithApplicationJson(
        new BoxplotPostResponseStream(new BoxplotPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsHeatmapResponse postAppsPassVisualizationsHeatmap(HeatmapPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsHeatmapResponse.respond200WithApplicationJson(
        new HeatmapPostResponseStream(new HeatmapPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsTwobytwoResponse postAppsPassVisualizationsTwobytwo(MosaicPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsTwobytwoResponse.respond200WithApplicationJson(
        new TwoByTwoPostResponseStream(new TwoByTwoPlugin().processRequest(entity))));
  }
  
  @DisableJackson
  @Override
  public PostAppsPassVisualizationsConttableResponse postAppsPassVisualizationsConttable(MosaicPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsConttableResponse.respond200WithApplicationJson(
        new ContTablePostResponseStream(new ContTablePlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAppsSampleVisualizationsRecordCountResponse postAppsSampleVisualizationsRecordCount(RecordCountPostRequest entity) {
    return wrapPlugin(() -> PostAppsSampleVisualizationsRecordCountResponse.respond200WithApplicationJson(
        new RecordCountPostResponseStream(new RecordCountPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAppsSampleVisualizationsMultiStreamResponse postAppsSampleVisualizationsMultiStream(MultiStreamPostRequest entity) {
    return wrapPlugin(() -> PostAppsSampleVisualizationsMultiStreamResponse.respond200WithTextPlain(
        new EntityTabularPostResponseStream(new MultiStreamPlugin().processRequest(entity))));
  }

  // Alpha div
  @DisableJackson
  @Override
  public PostAppsAlphaDivVisualizationsBoxplotResponse postAppsAlphaDivVisualizationsBoxplot(AlphaDivBoxplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsAlphaDivVisualizationsBoxplotResponse.respond200WithApplicationJson(
            new AlphaDivBoxplotPostResponseStream(new AlphaDivBoxplotPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAppsAlphaDivVisualizationsScatterplotResponse postAppsAlphaDivVisualizationsScatterplot(AlphaDivScatterplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsAlphaDivVisualizationsScatterplotResponse.respond200WithApplicationJson(
            new AlphaDivScatterplotPostResponseStream(new AlphaDivScatterplotPlugin().processRequest(entity))));
  }

  // Abundance
  @DisableJackson
  @Override
  public PostAppsAbundanceVisualizationsBoxplotResponse postAppsAbundanceVisualizationsBoxplot(AbundanceBoxplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsAbundanceVisualizationsBoxplotResponse.respond200WithApplicationJson(
            new AbundanceBoxplotPostResponseStream(new AbundanceBoxplotPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAppsAbundanceVisualizationsScatterplotResponse postAppsAbundanceVisualizationsScatterplot(AbundanceScatterplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsAbundanceVisualizationsScatterplotResponse.respond200WithApplicationJson(
            new AbundanceScatterplotPostResponseStream(new AbundanceScatterplotPlugin().processRequest(entity))));
  }

  // Beta div
  @DisableJackson
  @Override
  public PostAppsBetaDivVisualizationsScatterplotResponse postAppsBetaDivVisualizationsScatterplot(BetaDivScatterplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsBetaDivVisualizationsScatterplotResponse.respond200WithApplicationJson(
            new BetaDivScatterplotPostResponseStream(new BetaDivScatterplotPlugin().processRequest(entity))));
  }

}
