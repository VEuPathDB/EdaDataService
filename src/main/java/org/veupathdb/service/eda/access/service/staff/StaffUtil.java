package org.veupathdb.service.eda.access.service.staff;

import org.veupathdb.service.eda.access.model.StaffRow;
import org.veupathdb.service.eda.access.repo.DB;
import org.veupathdb.service.eda.access.service.user.UserUtil;

import java.sql.ResultSet;

public class StaffUtil
{
  private static StaffUtil instance = new StaffUtil();

  public StaffRow resultRowToStaffRow(final ResultSet rs) throws Exception {
    var out = new StaffRow();
    out.setStaffId(rs.getInt(DB.Column.Staff.StaffId));
    out.setOwner(rs.getBoolean(DB.Column.Staff.IsOwner));
    UserUtil.fillUser(rs, out);
    return out;
  }

  public static StaffUtil getInstance() {
    return instance;
  }

  public static StaffRow rowToStaff(final ResultSet rs) throws Exception {
    return getInstance().resultRowToStaffRow(rs);
  }
}
