package org.veupathdb.service.eda.ds.service;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;
import javax.xml.bind.ValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.functional.FunctionalInterfaces;
import org.veupathdb.lib.container.jaxrs.server.annotations.DisableJackson;
import org.veupathdb.service.eda.ds.plugin.BarplotPlugin;
import org.veupathdb.service.eda.ds.plugin.BoxplotPlugin;
import org.veupathdb.service.eda.ds.plugin.DateHistogramBinWidthPlugin;
import org.veupathdb.service.eda.ds.plugin.DateHistogramNumBinsPlugin;
import org.veupathdb.service.eda.ds.plugin.HeatmapPlugin;
import org.veupathdb.service.eda.ds.plugin.MapPlugin;
import org.veupathdb.service.eda.ds.plugin.MosaicPlugin;
import org.veupathdb.service.eda.ds.plugin.NumericHistogramBinWidthPlugin;
import org.veupathdb.service.eda.ds.plugin.NumericHistogramNumBinsPlugin;
import org.veupathdb.service.eda.ds.metadata.AppsMetadata;
import org.veupathdb.service.eda.ds.plugin.sample.RecordCountPlugin;
import org.veupathdb.service.eda.ds.plugin.ScatterplotPlugin;
import org.veupathdb.service.eda.ds.plugin.sample.MultiStreamPlugin;
import org.veupathdb.service.eda.generated.model.BarplotPostRequest;
import org.veupathdb.service.eda.generated.model.BarplotPostResponseStream;
import org.veupathdb.service.eda.generated.model.BoxplotPostRequest;
import org.veupathdb.service.eda.generated.model.BoxplotPostResponseStream;
import org.veupathdb.service.eda.generated.model.DateHistogramBinWidthPostRequest;
import org.veupathdb.service.eda.generated.model.DateHistogramBinWidthPostResponseStream;
import org.veupathdb.service.eda.generated.model.DateHistogramNumBinsPostRequest;
import org.veupathdb.service.eda.generated.model.DateHistogramNumBinsPostResponseStream;
import org.veupathdb.service.eda.generated.model.EntityTabularPostResponseStream;
import org.veupathdb.service.eda.generated.model.HeatmapPostRequest;
import org.veupathdb.service.eda.generated.model.HeatmapPostResponseStream;
import org.veupathdb.service.eda.generated.model.MapPostRequest;
import org.veupathdb.service.eda.generated.model.MapPostResponseStream;
import org.veupathdb.service.eda.generated.model.MosaicPostRequest;
import org.veupathdb.service.eda.generated.model.MosaicPostResponseStream;
import org.veupathdb.service.eda.generated.model.NumericHistogramBinWidthPostRequest;
import org.veupathdb.service.eda.generated.model.NumericHistogramBinWidthPostResponseStream;
import org.veupathdb.service.eda.generated.model.NumericHistogramNumBinsPostRequest;
import org.veupathdb.service.eda.generated.model.NumericHistogramNumBinsPostResponseStream;
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
  public PostAppsPassVisualizationsScatterplotResponse postAppsPassVisualizationsScatterplot(ScatterplotPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsScatterplotResponse.respond200WithApplicationJson(
        new ScatterplotPostResponseStream(new ScatterplotPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsDateHistogramBinWidthResponse postAppsPassVisualizationsDateHistogramBinWidth(DateHistogramBinWidthPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsDateHistogramBinWidthResponse.respond200WithApplicationJson(
        new DateHistogramBinWidthPostResponseStream(new DateHistogramBinWidthPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsDateHistogramNumBinsResponse postAppsPassVisualizationsDateHistogramNumBins(DateHistogramNumBinsPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsDateHistogramNumBinsResponse.respond200WithApplicationJson(
        new DateHistogramNumBinsPostResponseStream(new DateHistogramNumBinsPlugin().processRequest(entity))));
  }
  
  @DisableJackson
  @Override
  public PostAppsPassVisualizationsNumericHistogramBinWidthResponse postAppsPassVisualizationsNumericHistogramBinWidth(NumericHistogramBinWidthPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsNumericHistogramBinWidthResponse.respond200WithApplicationJson(
        new NumericHistogramBinWidthPostResponseStream(new NumericHistogramBinWidthPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAppsPassVisualizationsNumericHistogramNumBinsResponse postAppsPassVisualizationsNumericHistogramNumBins(NumericHistogramNumBinsPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsNumericHistogramNumBinsResponse.respond200WithApplicationJson(
        new NumericHistogramNumBinsPostResponseStream(new NumericHistogramNumBinsPlugin().processRequest(entity))));
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
  public PostAppsPassVisualizationsMosaicResponse postAppsPassVisualizationsMosaic(MosaicPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsMosaicResponse.respond200WithApplicationJson(
        new MosaicPostResponseStream(new MosaicPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAppsSampleVisualizationsRecordCountResponse postAppsSampleVisualizationsRecordCount(RecordCountPostRequest entity) {
    return wrapPlugin(() -> PostAppsSampleVisualizationsRecordCountResponse.respond200WithApplicationJson(
        new RecordCountPostResponseStream(new RecordCountPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAppsSampleVisualizationsMultiStreamResponse postAppsSampleVisualizationsMultiStream(RecordCountPostRequest entity) {
    return wrapPlugin(() -> PostAppsSampleVisualizationsMultiStreamResponse.respond200WithTextPlain(
        new EntityTabularPostResponseStream(new MultiStreamPlugin().processRequest(entity))));
  }

}
