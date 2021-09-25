package org.veupathdb.service.eda.ds.util;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.functional.FunctionalInterfaces.BiConsumerWithException;
import org.gusdb.fgputil.functional.FunctionalInterfaces.ConsumerWithException;
import org.rosuda.REngine.Rserve.RConnection;

public class RFileSetProcessor implements Iterable<RFileSetProcessor.RFileProcessingSpec> {

  public static class RFileProcessingSpec {

    public final String name;
    public final InputStream stream;
    public final Optional<Integer> maxAllowedRows;
    public final Boolean showMissingness;
    public final List<String> nonStrataColNames;
    public final ConsumerWithException<RConnection> fileReader;

    public RFileProcessingSpec(String name, InputStream stream, Boolean showMissingness, List<String> nonStrataColNames, Optional<TwoTuple<Optional<Integer>,BiConsumerWithException<String, RConnection>>> processingInfo) {
      this.name = name;
      this.stream = stream;
      this.maxAllowedRows = processingInfo.map(i -> i.getFirst()).orElse(Optional.empty());
      this.showMissingness = showMissingness;
      this.nonStrataColNames = nonStrataColNames;
      this.fileReader = conn -> processingInfo.map(i -> i.getSecond()).orElse((a,b) -> {}).accept(name, conn);
    }
  }

  private final Map<String,InputStream> _dataStreams;
  private final Map<String, Boolean> _showMissingnessMap = new HashMap<>();
  private final Map<String, List<String>> _nonStrataColNamesMap = new HashMap<>();
  private final Map<String, TwoTuple<Optional<Integer>,BiConsumerWithException<String, RConnection>>> _processingInfoMap = new HashMap<>();

  public RFileSetProcessor(Map<String, InputStream> dataStreams) {
    _dataStreams = dataStreams;
  }

  public RFileSetProcessor add(String name, BiConsumerWithException<String, RConnection> fileReader) {
    return add(name, null, "FALSE", null, fileReader);
  }

  public RFileSetProcessor add(String name, Integer maxAllowedRows, String showMissingness, List<String> nonStrataColNames, BiConsumerWithException<String, RConnection> fileReader) {
    Boolean booleanShowMissingness = showMissingness.equals("FALSE") ? false : true;
    
    if (!_dataStreams.containsKey(name)) {
      throw new IllegalArgumentException("name parameter value '" + name + "' must be contained in dataStreams map passed to constructor");
    }
    _showMissingnessMap.put(name, booleanShowMissingness);
    _nonStrataColNamesMap.put(name, nonStrataColNames);
    _processingInfoMap.put(name, new TwoTuple<>(Optional.ofNullable(maxAllowedRows), fileReader));
    return this;
  }

  public RFileSetProcessor add(String name, Integer maxAllowedRows, BiConsumerWithException<String, RConnection> fileReader) {
     return add(name, maxAllowedRows, "FALSE", null, fileReader);
   }

  @Override
  public Iterator<RFileProcessingSpec> iterator() {
    return _dataStreams.entrySet().stream()
      .map(entry ->
        new RFileProcessingSpec(entry.getKey(), entry.getValue(),
            _showMissingnessMap.get(entry.getKey()),
            _nonStrataColNamesMap.get(entry.getKey()),
            Optional.ofNullable(_processingInfoMap.get(entry.getKey()))))
      .collect(Collectors.toList())
      .iterator();
  }
}
