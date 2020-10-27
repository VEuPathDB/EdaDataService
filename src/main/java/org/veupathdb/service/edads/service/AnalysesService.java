package org.veupathdb.service.edads.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;
import javax.xml.bind.ValidationException;
import org.gusdb.fgputil.functional.FunctionalInterfaces;
import org.veupathdb.lib.container.jaxrs.server.annotations.DisableJackson;
import org.veupathdb.service.edads.generated.model.AnalysesGetResponse;
import org.veupathdb.service.edads.generated.model.AnalysesGetResponseImpl;
import org.veupathdb.service.edads.generated.model.AnalysisOverview;
import org.veupathdb.service.edads.generated.model.AnalysisOverviewImpl;
import org.veupathdb.service.edads.generated.model.HistogramPostRequest;
import org.veupathdb.service.edads.generated.model.HistogramPostResponseStream;
import org.veupathdb.service.edads.generated.model.RecordCountPostRequest;
import org.veupathdb.service.edads.generated.model.RecordCountPostResponseStream;
import org.veupathdb.service.edads.generated.model.ScatterplotPostRequest;
import org.veupathdb.service.edads.generated.model.ScatterplotPostResponseStream;
import org.veupathdb.service.edads.generated.resources.Analyses;
import org.veupathdb.service.edads.plugin.histogram.HistogramPlugin;
import org.veupathdb.service.edads.plugin.recordcount.RecordCountPlugin;
import org.veupathdb.service.edads.plugin.scatterplot.ScatterplotPlugin;

public class AnalysesService implements Analyses {

  private static final String[] ANALYSIS_NAMES = {
    "record-count",
    "scatterplot",
    "histogram"
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
  public PostAnalysesHistogramResponse postAnalysesHistogram(HistogramPostRequest entity) {
    return wrapPlugin(() -> PostAnalysesHistogramResponse.respond200WithApplicationJson(
        new HistogramPostResponseStream(new HistogramPlugin().processRequest(entity))));
  }
}
