package org.veupathdb.service.eda.common.client;

import jakarta.ws.rs.ProcessingException;
import org.apache.log4j.Logger;
import org.gusdb.fgputil.AutoCloseableList;
import org.gusdb.fgputil.client.ResponseFuture;
import org.gusdb.fgputil.functional.FunctionalInterfaces;
import org.gusdb.fgputil.iterator.CloseableIterator;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.client.spec.StreamSpecValidator;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.generated.model.APIFilter;
import org.veupathdb.service.eda.generated.model.VariableSpec;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.gusdb.fgputil.functional.Functions.cSwallow;

public abstract class StreamingDataClient extends ServiceClient {
  private static final Logger LOG = Logger.getLogger(StreamingDataClient.class);

  public abstract StreamSpecValidator getStreamSpecValidator();

  public abstract String varToColumnHeader(VariableSpec var);

  public abstract ResponseFuture getTabularDataStream(
      ReferenceMetadata metadata,
      List<APIFilter> subset,
      StreamSpec spec) throws ProcessingException;

  protected StreamingDataClient(String serviceBaseUrl, Entry<String, String> authHeader) {
    super(serviceBaseUrl, authHeader);
  }

  public static void buildAndProcessStreams(
      List<StreamSpec> requiredStreams,
      Function<StreamSpec, ResponseFuture> streamGenerator,
      FunctionalInterfaces.ConsumerWithException<Map<String, InputStream>> streamProcessor) {
    try (AutoCloseableList<InputStream> dataStreams = buildDataStreams(requiredStreams, streamGenerator)) {
      // convert auto-closeable list into a named stream map for processing
      Map<String,InputStream> streamMap = new LinkedHashMap<>();
      for (int i = 0; i < dataStreams.size(); i++) {
        streamMap.put(requiredStreams.get(i).getStreamName(), dataStreams.get(i));
      }
      cSwallow(streamProcessor).accept(streamMap);
    }
  }

  public static void buildAndProcessIteratorStreams(ArrayList<StreamSpec> requiredStreams,
                                                    Function<StreamSpec, CloseableIterator<Map<String, String>>> streamGenerator,
                                                    FunctionalInterfaces.ConsumerWithException<Map<String, CloseableIterator<Map<String, String>>>> streamProcessor) {
    try (AutoCloseableList<CloseableIterator<Map<String, String>>> streams = new AutoCloseableList<>(requiredStreams.stream()
        .map(streamGenerator::apply)
        .collect(Collectors.toList()))) {
      // convert auto-closeable list into a named stream map for processing
      Map<String, CloseableIterator<Map<String, String>>> streamMap = new LinkedHashMap<>();

      for (int i = 0; i < streams.size(); i++) {
        streamMap.put(requiredStreams.get(i).getStreamName(), streams.get(i));
      }
      cSwallow(streamProcessor).accept(streamMap);
    }
  }

  private static AutoCloseableList<InputStream> buildDataStreams(
      List<StreamSpec> requiredStreams,
      Function<StreamSpec,ResponseFuture> streamGenerator) {
    AutoCloseableList<InputStream> dataStreams = new AutoCloseableList<>();
    Map<String, ResponseFuture> responses = new HashMap<>();
    try {
      // parallelize the calls
      for (StreamSpec spec : requiredStreams) {
        responses.put(spec.getStreamName(), streamGenerator.apply(spec));
      }

      // wait for all to complete
      ResponseFuture.waitForAll(responses.values());

      // get results
      for (StreamSpec spec : requiredStreams) {
        dataStreams.add(responses.get(spec.getStreamName()).getInputStream());
        responses.remove(spec.getStreamName());
      }
      return dataStreams;
    }
    catch (Exception e) {
      // if exception occurs while creating streams; need to clean up (two parts)
      // 1. cancel remaining responses not opened
      for (ResponseFuture response : responses.values()) {
        response.cancel();
      }
      // 2. close any that successfully opened
      dataStreams.close();
      // throw as a runtime exception
      throw new RuntimeException("Unable to fetch all required data", e);
    }
  }
}
