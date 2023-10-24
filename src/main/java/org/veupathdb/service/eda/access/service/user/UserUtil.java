package org.veupathdb.service.eda.access.service.user;

import org.veupathdb.service.eda.access.model.UserRow;
import org.veupathdb.service.eda.access.repo.DB;

import java.sql.ResultSet;

public class UserUtil
{
  private static UserUtil instance = new UserUtil();

  public void fillUserRow(final ResultSet rs, final UserRow row) throws Exception {
    row.setUserId(rs.getLong(DB.Column.EndUser.UserId));
    row.setEmail(rs.getString(DB.Column.Accounts.Email));
    row.setFirstName(rs.getString(DB.Column.Misc.FirstName));
    row.setLastName(rs.getString(DB.Column.Misc.LastName));
    row.setOrganization(rs.getString(DB.Column.Misc.Organization));
  }

  public static UserUtil getInstance() {
    return instance;
  }

  public static void fillUser(final ResultSet rs, final UserRow row) throws Exception {
    getInstance().fillUserRow(rs, row);
  }
}
