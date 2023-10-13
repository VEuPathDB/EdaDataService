package org.veupathdb.service.eda.access.service.history.model;

import java.sql.ResultSet;

public class HistoryUserRow
{
  public final long   userID;
  public final String firstName;
  public final String lastName;
  public final String organization;
  public final String email;

  public HistoryUserRow(ResultSet rs) throws Exception {
    firstName    = rs.getString(1);
    lastName     = rs.getString(2);
    organization = rs.getString(3);
    email        = rs.getString(4);
    userID       = rs.getLong(5);
  }
}
