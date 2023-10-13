package org.veupathdb.service.eda.access.util;

import io.vulpine.lib.query.util.RowParser;
import io.vulpine.lib.query.util.StatementPreparer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public class SqlUtil
{
  /**
   * Returns a wrapper {@link RowParser} that calls the given function if the
   * {@link ResultSet} has a row, wrapping the result in an
   * {@link Optional} instance.  If the {@code ResultSet} has no rows, the
   * wrapper will return an empty {@code Optional} instance.
   *
   * @param fn  {@code RowParser} to wrap.
   * @param <V> Return type for both the given row parser and the output
   *            {@code Optional} generic type.
   *
   * @return a new {@code RowParser} instance wrapping the given input function.
   *
   * @throws NullPointerException if the given {@code RowParser} instance is
   *                              null.
   */
  public static <V> RowParser<Optional<V>> optParser(final RowParser<V> fn) {
    Objects.requireNonNull(fn);
    return rs -> rs.next() ? Optional.of(fn.parse(rs)) : Optional.empty();
  }

  public static <V> RowParser<V> reqParser(final RowParser<V> fn) {
    Objects.requireNonNull(fn);
    return rs -> {
      if (!rs.next())
        throw new IllegalStateException();
      return fn.parse(rs);
    };
  }

  public static String parseSingleString(final ResultSet rs) throws Exception {
    try {
      return rs.getString(1);
    } catch (SQLException e) {
      throw new Exception("Failed to look up string value at position 1", e);
    }
  }

  public static int parseSingleInt(final ResultSet rs) throws Exception {
    try {
      return rs.getInt(1);
    } catch (SQLException e) {
      throw new Exception("Failed to look up int value at position 1", e);
    }
  }

  public static long parseSingleLong(final ResultSet rs) throws Exception {
    try {
      return rs.getLong(1);
    } catch (SQLException e) {
      throw new Exception("Failed to look up long value at position 1", e);
    }
  }

  public static StatementPreparer prepareSingleInt(final int value) {
    return ps -> ps.setInt(1, value);
  }

  public static StatementPreparer prepareSingleLong(final long value) {
    return ps -> ps.setLong(1, value);
  }

  public static StatementPreparer prepareSingleString(final String value) {
    return ps -> ps.setString(1, value);
  }
}
