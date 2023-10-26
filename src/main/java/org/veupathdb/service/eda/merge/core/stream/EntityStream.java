package org.veupathdb.service.eda.merge.core.stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.DelimitedDataParser;
import org.gusdb.fgputil.iterator.CloseableIterator;
import org.gusdb.fgputil.json.JsonUtil;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.generated.model.VariableSpecImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.FormatUtil.TAB;

/**
 * Base class for entity streams; handles reading tabular data into a map for
 * each row, making a call to add inherited and derived vars (actual implementation
 * handled by a subclass), and caching the last produced row for inspection before
 * delivery.  This row may be delivered more than once depending on the location of
 * this data stream in the entity tree.
 *
 * Serves as a base class for stream processor nodes that abstracts away the
 * reading of the tabular data (leaving tree-related and derived variable logic
 * to a subclass).
 *
 * The lifecycle of this class is:
 * 1. construction
 * 2. assignment of the stream spec (typically by the subclass in its constructor)
 * 3. assignment of the data stream
 * 4. reading tabular rows from stream, incorporating DVs, and outputting data Maps as requested
 */
public class EntityStream implements Iterator<Map<String,String>> {

  private static final Logger LOG = LogManager.getLogger(EntityStream.class);

  // final fields
  protected final ReferenceMetadata _metadata;

  // fields set up by assignment of stream spec
  private StreamSpec _streamSpec;
  private String _entityIdColumnName;
  private List<VariableDef> _expectedNativeColumns;
  private CloseableIterator<Map<String, String>> _dataStream;

  // fields set up by the assignment of the data stream
  // caches the last row read from the scanner (null if no more rows)
  private Map<String, String> _lastRowRead;

  protected EntityStream(ReferenceMetadata metadata) {
    _metadata = metadata;
  }

  protected EntityStream setStreamSpec(StreamSpec streamSpec) {
    LOG.info("Initializing " + getClass().getSimpleName() + " for entity " + streamSpec.getEntityId());
    _streamSpec = streamSpec;
    EntityDef entity = _metadata.getEntity(streamSpec.getEntityId()).orElseThrow();
    // cache the name of the column used to identify records that match the current row
    _entityIdColumnName = VariableDef.toDotNotation(entity.getIdColumnDef());
    LOG.info("Entity ID column name: " + _entityIdColumnName);
    _expectedNativeColumns = _metadata.getTabularColumns(entity, streamSpec);
    return this;
  }

  protected StreamSpec getStreamSpec() {
    return _streamSpec;
  }

  protected String getEntityIdColumnName() {
    return _entityIdColumnName;
  }

  public void acceptDataStreams(Map<String, CloseableIterator<Map<String, String>>> dataStreams) {
    _dataStream = dataStreams.get(_streamSpec.getStreamName());
    if (_dataStream == null) // not found!
      throw new IllegalStateException("Stream with name " + _streamSpec.getStreamName() + " expected but not distributed.");
    // remove the stream from the map; then enables later checking of whether all streams were distributed
    dataStreams.remove(_streamSpec.getStreamName());
    _lastRowRead = readRow();
    LOG.info("Received a stream with stream spec {} and headers {}", _streamSpec, _lastRowRead.keySet());
  }

  /**
   * By default this class does not apply derived or inherited vars.  This way it can still be used stand-alone to
   * process computed variable tabular data streams, and could be used (but is not) in cases where no derived or
   * inherited vars exist.
   *
   * @param row row of data read from the tabular data stream
   * @return row of data with inherited and derived variables applied.  This CAN be the same object as the parameter.
   */
  protected Map<String, String> applyDerivedVars(Map<String, String> row) {
    return row;
  }

  // returns null if no more rows
  private Map<String, String> readRow() {
    return !_dataStream.hasNext() ? null : applyDerivedVars(_dataStream.next());
  }

  @Override
  public boolean hasNext() {
    return _lastRowRead != null;
  }

  @Override
  public Map<String, String> next() {
    if (!hasNext()) {
      throw new NoSuchElementException("No rows remain.");
    }
    Map<String, String> lastRow = _lastRowRead;
    _lastRowRead = readRow();
    return lastRow;
  }

  /**
   * If the stored row matches the predicate, then returns it but does not iterate (iteration must be
   * handled independently by extra calls to <code>next()</code>).  If no more rows or if stored row
   * does not match the predicate, returns an empty optional (and still does not iterate).
   *
   * This is used for inheriting parent vars across multiple children of that parent (i.e. whose IDs
   * match the parent's ID).  The parent row is retained until a child comes along that does not
   * match it.
   *
   */
  public Optional<Map<String, String>> getPreviousRowIf(Predicate<Map<String, String>> condition) {
    return hasNext() && condition.test(_lastRowRead)
        ? Optional.of(_lastRowRead)
        : Optional.empty();
  }

  /**
   * If the stored row matches the predicate, then returns it and reads the next row, storing it.  If no
   * more rows or if stored row does not match the predicate, returns an empty optional and does not iterate.
   *
   * This is used for reductions to efficiently continue reading child rows that match a parent ID until a
   * child row is found that does not match the parent ID.
   */
  public Optional<Map<String, String>> getNextRowIf(Predicate<Map<String, String>> condition) {
    return hasNext() && condition.test(_lastRowRead)
        ? Optional.of(next())
        : Optional.empty();
  }

  @Override
  public String toString() {
    return toString(0);
  }

  public String toString(int indentSize) {
    String indent = " ".repeat(indentSize);
    return
        indent + "{" + NL +
            indent + "  entityIdColumnName: " + _entityIdColumnName + NL +
            indent + "  expectedNativeColumns: [" + NL +
            _expectedNativeColumns.stream().map(c -> indent + "    " + c.toString() + NL).collect(Collectors.joining()) +
            indent + "  streamSpec: " + NL + _streamSpec.toString(indentSize + 2) + NL +
            indent + "  filtersOverride: " + _streamSpec.getFiltersOverride().map(JsonUtil::serializeObject).orElse("none") + NL +
            indent + "}";
  }

}
