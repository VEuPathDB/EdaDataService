package org.veupathdb.service.edads.service;

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
import org.veupathdb.service.edads.generated.model.AnalysesGetResponse;
import org.veupathdb.service.edads.generated.model.AnalysesGetResponseImpl;
import org.veupathdb.service.edads.generated.model.AnalysisOverview;
import org.veupathdb.service.edads.generated.model.AnalysisOverviewImpl;
import org.veupathdb.service.edads.generated.model.BarplotPostRequest;
import org.veupathdb.service.edads.generated.model.BarplotPostResponseStream;
import org.veupathdb.service.edads.generated.model.BoxplotPostRequest;
import org.veupathdb.service.edads.generated.model.BoxplotPostResponseStream;
import org.veupathdb.service.edads.generated.model.HeatmapPostRequest;
import org.veupathdb.service.edads.generated.model.HeatmapPostResponseStream;
import org.veupathdb.service.edads.generated.model.HistogramBinWidthPostRequest;
import org.veupathdb.service.edads.generated.model.HistogramNumBinsPostRequest;
import org.veupathdb.service.edads.generated.model.HistogramBinWidthPostResponseStream;
import org.veupathdb.service.edads.generated.model.HistogramNumBinsPostResponseStream;
import org.veupathdb.service.edads.generated.model.MosaicPostRequest;
import org.veupathdb.service.edads.generated.model.MosaicPostResponseStream;
import org.veupathdb.service.edads.generated.model.RecordCountPostRequest;
import org.veupathdb.service.edads.generated.model.RecordCountPostResponseStream;
import org.veupathdb.service.edads.generated.model.ScatterplotPostRequest;
import org.veupathdb.service.edads.generated.model.ScatterplotPostResponseStream;
import org.veupathdb.service.edads.generated.resources.Analyses;
import org.veupathdb.service.edads.plugin.BarplotPlugin;
import org.veupathdb.service.edads.plugin.BoxplotPlugin;
import org.veupathdb.service.edads.plugin.HeatmapPlugin;
import org.veupathdb.service.edads.plugin.HistogramBinWidthPlugin;
import org.veupathdb.service.edads.plugin.HistogramNumBinsPlugin;
import org.veupathdb.service.edads.plugin.MosaicPlugin;
import org.veupathdb.service.edads.plugin.RecordCountPlugin;
import org.veupathdb.service.edads.plugin.ScatterplotPlugin;


public class AnalysesService implements Analyses {

  private static final Logger LOG = LogManager.getLogger(AnalysesService.class);

  private static final String[] ANALYSIS_NAMES = {
    "record-count",
    "scatterplot",
    "histogram-num-bins",
    "histogram-bin-width",
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
  public PostAnalysesScatterplotResponse postAnalysesScatterplot(ScatterplotPostRequest entity) {
    return wrapPlugin(() -> PostAnalysesScatterplotResponse.respond200WithApplicationJson(
        new ScatterplotPostResponseStream(new ScatterplotPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAnalysesHistogramBinWidthResponse postAnalysesHistogramBinWidth(HistogramBinWidthPostRequest entity) {
    return wrapPlugin(() -> PostAnalysesHistogramBinWidthResponse.respond200WithApplicationJson(
        new HistogramBinWidthPostResponseStream(new HistogramBinWidthPlugin().processRequest(entity))));
  }

  @DisableJackson
  @Override
  public PostAnalysesHistogramNumBinsResponse postAnalysesHistogramNumBins(HistogramNumBinsPostRequest entity) {
    return wrapPlugin(() -> PostAnalysesHistogramNumBinsResponse.respond200WithApplicationJson(
        new HistogramNumBinsPostResponseStream(new HistogramNumBinsPlugin().processRequest(entity))));
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
