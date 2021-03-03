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
import org.veupathdb.service.eda.generated.model.AnalysesGetResponse;
import org.veupathdb.service.eda.generated.model.AnalysesGetResponseImpl;
import org.veupathdb.service.eda.generated.model.AnalysisOverview;
import org.veupathdb.service.eda.generated.model.AnalysisOverviewImpl;
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
import org.veupathdb.service.eda.generated.resources.Analyses;
import org.veupathdb.service.eda.ds.plugin.BarplotPlugin;
import org.veupathdb.service.eda.ds.plugin.BoxplotPlugin;
import org.veupathdb.service.eda.ds.plugin.HeatmapPlugin;
import org.veupathdb.service.eda.ds.plugin.MapPlugin;
import org.veupathdb.service.eda.ds.plugin.MosaicPlugin;


public class AnalysisService implements Analyses {

  private static final Logger LOG = LogManager.getLogger(AnalysisService.class);

  // NOTE: these names should match the url segments defined in the api.raml
  private static final String[] ANALYSIS_NAMES = {
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
    "mosaicplot"
  };

  private static final List<AnalysisOverview> ANALYSES_LIST = Arrays.stream(ANALYSIS_NAMES).map(name ->
    new AnalysisOverviewImpl() {
      @Override
      public String getName() {
        return name;
      }
    }).collect(Collectors.toList());

  private static final AnalysesGetResponse ANALYSES_RESPONSE = new AnalysesGetResponseImpl() {
    @Override
    public List<AnalysisOverview> getAnalyses() {
      return ANALYSES_LIST;
    }
  };

  @Override
  public GetAnalysesResponse getAnalyses() {
    return GetAnalysesResponse.respond200WithApplicationJson(ANALYSES_RESPONSE);
  }

  private <T> T wrapPlugin(FunctionalInterfaces.SupplierWithException<T> supplier) {
    try {
      return supplier.get();
    }
    catch (ValidationException e) {
      throw new BadRequestException(e.getMessage());
    }
    catch (Exception e) {
      LOG.error("Could not execute analysis.", e);
      throw new ServerErrorException(e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @DisableJackson
  @Override
  public PostAnalysesRecordCountResponse postAnalysesRecordCount(RecordCountPostRequest entity) {
    return wrapPlugin(() -> PostAnalysesRecordCountResponse.respond200WithApplicationJson(
        new RecordCountPostResponseStream(new RecordCountPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAnalysesMapMarkersResponse postAnalysesMapMarkers(MapPostRequest entity) {
    return wrapPlugin(() -> PostAnalysesMapMarkersResponse.respond200WithApplicationJson(
        new MapPostResponseStream(new MapPlugin().processRequest(entity))));
  }
  
  @DisableJackson
  @Override
  public PostAnalysesScatterplotResponse postAnalysesScatterplot(ScatterplotPostRequest entity) {
    return wrapPlugin(() -> PostAnalysesScatterplotResponse.respond200WithApplicationJson(
        new ScatterplotPostResponseStream(new ScatterplotPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAnalysesDateHistogramBinWidthResponse postAnalysesDateHistogramBinWidth(DateHistogramBinWidthPostRequest entity) {
    return wrapPlugin(() -> PostAnalysesDateHistogramBinWidthResponse.respond200WithApplicationJson(
        new DateHistogramBinWidthPostResponseStream(new DateHistogramBinWidthPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAnalysesDateHistogramNumBinsResponse postAnalysesDateHistogramNumBins(DateHistogramNumBinsPostRequest entity) {
    return wrapPlugin(() -> PostAnalysesDateHistogramNumBinsResponse.respond200WithApplicationJson(
        new DateHistogramNumBinsPostResponseStream(new DateHistogramNumBinsPlugin().processRequest(entity))));
  }
  
  @DisableJackson
  @Override
  public PostAnalysesNumericHistogramBinWidthResponse postAnalysesNumericHistogramBinWidth(NumericHistogramBinWidthPostRequest entity) {
    return wrapPlugin(() -> PostAnalysesNumericHistogramBinWidthResponse.respond200WithApplicationJson(
        new NumericHistogramBinWidthPostResponseStream(new NumericHistogramBinWidthPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAnalysesNumericHistogramNumBinsResponse postAnalysesNumericHistogramNumBins(NumericHistogramNumBinsPostRequest entity) {
    return wrapPlugin(() -> PostAnalysesNumericHistogramNumBinsResponse.respond200WithApplicationJson(
        new NumericHistogramNumBinsPostResponseStream(new NumericHistogramNumBinsPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAnalysesBarplotResponse postAnalysesBarplot(BarplotPostRequest entity) {
    return wrapPlugin(() -> PostAnalysesBarplotResponse.respond200WithApplicationJson(
        new BarplotPostResponseStream(new BarplotPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAnalysesBoxplotResponse postAnalysesBoxplot(BoxplotPostRequest entity) {
    return wrapPlugin(() -> PostAnalysesBoxplotResponse.respond200WithApplicationJson(
        new BoxplotPostResponseStream(new BoxplotPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAnalysesHeatmapResponse postAnalysesHeatmap(HeatmapPostRequest entity) {
    return wrapPlugin(() -> PostAnalysesHeatmapResponse.respond200WithApplicationJson(
        new HeatmapPostResponseStream(new HeatmapPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAnalysesMosaicResponse postAnalysesMosaic(MosaicPostRequest entity) {
    return wrapPlugin(() -> PostAnalysesMosaicResponse.respond200WithApplicationJson(
        new MosaicPostResponseStream(new MosaicPlugin().processRequest(entity))));
  }

}
