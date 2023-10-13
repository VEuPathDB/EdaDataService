package org.veupathdb.service.eda.access.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class PsBuilder
{
  private final List<Value> values;

  public PsBuilder() {
    values = new ArrayList<>();
  }

  public PsBuilder setByte(final byte value) {
    values.add(new Value(value, Types.TINYINT));
    return this;
  }

  public PsBuilder setShort(final short value) {
    values.add(new Value(value, Types.SMALLINT));
    return this;
  }

  public PsBuilder setInt(final int value) {
    values.add(new Value(value, Types.INTEGER));
    return this;
  }

  public PsBuilder setLong(final long value) {
    values.add(new Value(value, Types.BIGINT));
    return this;
  }

  public PsBuilder setString(final String value) {
    values.add(new Value(value, Types.VARCHAR));
    return this;
  }

  public PsBuilder setObject(final Object value, final int type) {
    values.add(new Value(value, type));
    return this;
  }

  public PsBuilder setValue(final Value value) {
    values.add(value);
    return this;
  }

  public PsBuilder setBoolean(final boolean value) {
    values.add(new Value(value, Types.TINYINT));
    return this;
  }

  public void build(final PreparedStatement ps) throws SQLException {
    for (int i = 0; i < values.size(); i++) {
      Value val = values.get(i);
      ps.setObject(i + 1, val.getValue(), val.getType());
    }
  }

  public static class Value
  {
    private final Object value;
    private final int    type;

    public Value(final Object v, final int t) {
      value = v;
      type  = t;
    }

    public Object getValue() {
      return value;
    }

    public int getType() {
      return type;
    }
  }
}
