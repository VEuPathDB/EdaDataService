package org.veupathdb.service.eda.ds.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;
import javax.xml.bind.ValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.functional.FunctionalInterfaces;
import org.veupathdb.lib.container.jaxrs.server.annotations.DisableJackson;
import org.veupathdb.service.eda.ds.plugin.DateHistogramBinWidthPlugin;
import org.veupathdb.service.eda.ds.plugin.DateHistogramNumBinsPlugin;
import org.veupathdb.service.eda.ds.plugin.NumericHistogramBinWidthPlugin;
import org.veupathdb.service.eda.ds.plugin.NumericHistogramNumBinsPlugin;
import org.veupathdb.service.eda.ds.plugin.RecordCountPlugin;
import org.veupathdb.service.eda.ds.plugin.ScatterplotPlugin;
import org.veupathdb.service.eda.generated.model.AppsGetResponse;
import org.veupathdb.service.eda.generated.model.AppsGetResponseImpl;
import org.veupathdb.service.eda.generated.model.AppsOverview;
import org.veupathdb.service.eda.generated.model.AppsOverviewImpl;
import org.veupathdb.service.eda.generated.model.PassAppVisualizationsGetResponse;
import org.veupathdb.service.eda.generated.model.PassAppVisualizationsGetResponseImpl;
import org.veupathdb.service.eda.generated.model.PassAppVisualizationsOverview;
import org.veupathdb.service.eda.generated.model.PassAppVisualizationsOverviewImpl;
import org.veupathdb.service.eda.generated.model.BarplotPostRequest;
import org.veupathdb.service.eda.generated.model.BarplotPostResponseStream;
import org.veupathdb.service.eda.generated.model.BoxplotPostRequest;
import org.veupathdb.service.eda.generated.model.BoxplotPostResponseStream;
import org.veupathdb.service.eda.generated.model.HeatmapPostRequest;
import org.veupathdb.service.eda.generated.model.HeatmapPostResponseStream;
import org.veupathdb.service.eda.generated.model.DateHistogramBinWidthPostRequest;
import org.veupathdb.service.eda.generated.model.DateHistogramNumBinsPostRequest;
import org.veupathdb.service.eda.generated.model.DateHistogramBinWidthPostResponseStream;
import org.veupathdb.service.eda.generated.model.DateHistogramNumBinsPostResponseStream;
import org.veupathdb.service.eda.generated.model.NumericHistogramBinWidthPostRequest;
import org.veupathdb.service.eda.generated.model.NumericHistogramNumBinsPostRequest;
import org.veupathdb.service.eda.generated.model.NumericHistogramBinWidthPostResponseStream;
import org.veupathdb.service.eda.generated.model.NumericHistogramNumBinsPostResponseStream;
import org.veupathdb.service.eda.generated.model.MapPostRequest;
import org.veupathdb.service.eda.generated.model.MapPostResponseStream;
import org.veupathdb.service.eda.generated.model.MosaicPostRequest;
import org.veupathdb.service.eda.generated.model.MosaicPostResponseStream;
import org.veupathdb.service.eda.generated.model.RecordCountPostRequest;
import org.veupathdb.service.eda.generated.model.RecordCountPostResponseStream;
import org.veupathdb.service.eda.generated.model.ScatterplotPostRequest;
import org.veupathdb.service.eda.generated.model.ScatterplotPostResponseStream;
import org.veupathdb.service.eda.generated.resources.Apps;
import org.veupathdb.service.eda.ds.plugin.BarplotPlugin;
import org.veupathdb.service.eda.ds.plugin.BoxplotPlugin;
import org.veupathdb.service.eda.ds.plugin.HeatmapPlugin;
import org.veupathdb.service.eda.ds.plugin.MapPlugin;
import org.veupathdb.service.eda.ds.plugin.MosaicPlugin;

public class AppsService implements Apps {

  private static final Logger LOG = LogManager.getLogger(AppsService.class);

  private static final String[] APP_NAMES = {
      "pass"
    };
  
  // NOTE: these names should match the url segments defined in the api.raml
  private static final String[] PASS_APP_VISUALIZATION_NAMES = {
    "record-count",
    "map-markers",
    "scatterplot",
    "date-histogram-num-bins",
    "date-histogram-bin-width",
    "numeric-histogram-num-bins",
    "numeric-histogram-bin-width",
    "barplot",
    "boxplot",
    "heatmap",
    "mosaic"
  };

  private static final List<AppsOverview> APPS_LIST = Arrays.stream(APP_NAMES).map(name ->
    new AppsOverviewImpl() {
      @Override
      public String getName() {
        return name;
      }
    }).collect(Collectors.toList());

  private static final AppsGetResponse APPS_RESPONSE = new AppsGetResponseImpl() {
    @Override
    public List<AppsOverview> getApps() {
      return APPS_LIST;
    }
  };

  @Override
  public GetAppsResponse getApps() {
    return GetAppsResponse.respond200WithApplicationJson(APPS_RESPONSE);
  }
  
  private static final List<PassAppVisualizationsOverview> PASS_APP_VISUALIZATIONS_LIST = Arrays.stream(PASS_APP_VISUALIZATION_NAMES).map(name ->
    new PassAppVisualizationsOverviewImpl() {
      @Override
      public String getName() {
        return name;
      }
    }).collect(Collectors.toList());

  private static final PassAppVisualizationsGetResponse PASS_APP_VISUALIZATIONS_RESPONSE = new PassAppVisualizationsGetResponseImpl() {
    @Override
    public List<PassAppVisualizationsOverview> getVisualizations() {
      return PASS_APP_VISUALIZATIONS_LIST;
    }
  };

  @Override
  public GetAppsPassVisualizationsResponse getAppsPassVisualizations() {
    return GetAppsPassVisualizationsResponse.respond200WithApplicationJson(PASS_APP_VISUALIZATIONS_RESPONSE);
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
  public PostAppsPassVisualizationsRecordCountResponse postAppsPassVisualizationsRecordCount(RecordCountPostRequest entity) {
    return wrapPlugin(() -> PostAppsPassVisualizationsRecordCountResponse.respond200WithApplicationJson(
        new RecordCountPostResponseStream(new RecordCountPlugin().processRequest(entity))));
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

}
