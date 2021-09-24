package org.veupathdb.service.eda.ds.util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.gusdb.fgputil.Tuples.ThreeTuple;
import org.gusdb.fgputil.functional.FunctionalInterfaces.BiConsumerWithException;
import org.gusdb.fgputil.functional.FunctionalInterfaces.ConsumerWithException;
import org.rosuda.REngine.Rserve.RConnection;

public class RFileSetProcessor implements Iterable<RFileSetProcessor.RFileProcessingSpec> {

  public static class RFileProcessingSpec {

    public final String name;
    public final InputStream stream;
    public final Optional<Integer> maxAllowedRows;
    public final Boolean showMissingness;
    public final ConsumerWithException<RConnection> fileReader;

    public RFileProcessingSpec(String name, InputStream stream, Optional<ThreeTuple<Optional<Integer>,Boolean,BiConsumerWithException<String, RConnection>>> processingInfo) {
      this.name = name;
      this.stream = stream;
      this.maxAllowedRows = processingInfo.map(i -> i.getFirst()).orElse(Optional.empty());
      this.showMissingness = processingInfo.map(i -> i.getSecond()).orElse(false);
      this.fileReader = conn -> processingInfo.map(i -> i.getThird()).orElse((a,b) -> {}).accept(name, conn);
    }
  }

  private final Map<String,InputStream> _dataStreams;
  private final Map<String, ThreeTuple<Optional<Integer>,Boolean,BiConsumerWithException<String, RConnection>>> _processingInfoMap = new HashMap<>();

  public RFileSetProcessor(Map<String, InputStream> dataStreams) {
    _dataStreams = dataStreams;
  }

  public RFileSetProcessor add(String name, BiConsumerWithException<String, RConnection> fileReader) {
    return add(name, null, "FALSE", fileReader);
  }

  public RFileSetProcessor add(String name, Integer maxAllowedRows, String showMissingness, BiConsumerWithException<String, RConnection> fileReader) {
    Boolean booleanShowMissingness = showMissingness.equals("FALSE") ? false : true;
    
    if (!_dataStreams.containsKey(name)) {
      throw new IllegalArgumentException("name parameter value '" + name + "' must be contained in dataStreams map passed to constructor");
    }
    _processingInfoMap.put(name, new ThreeTuple<>(Optional.ofNullable(maxAllowedRows), booleanShowMissingness, fileReader));
    return this;
  }

   public RFileSetProcessor add(String name, Integer maxAllowedRows, BiConsumerWithException<String, RConnection> fileReader) {
     add(name, maxAllowedRows, "FALSE", fileReader);
   }

  @Override
  public Iterator<RFileProcessingSpec> iterator() {
    return _dataStreams.entrySet().stream()
      .map(entry ->
        new RFileProcessingSpec(entry.getKey(), entry.getValue(),
            Optional.ofNullable(_processingInfoMap.get(entry.getKey()))))
      .collect(Collectors.toList())
      .iterator();
  }
}
