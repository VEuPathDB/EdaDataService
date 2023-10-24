package org.veupathdb.service.eda.access.service.permissions;

import org.veupathdb.service.eda.generated.model.DatasetPermissionEntry;
import org.veupathdb.service.eda.generated.model.DatasetPermissionEntryImpl;
import org.veupathdb.service.eda.generated.model.DatasetPermissionLevel;

class PermissionUtil
{
  private static PermissionUtil instance;

  DatasetPermissionEntry bool2entry(boolean val) {
    var out = new DatasetPermissionEntryImpl();

    out.setType(DatasetPermissionLevel.PROVIDER);
    out.setIsManager(val);

    return out;
  }

  DatasetPermissionEntry string2entry(String e) {
    var out = new DatasetPermissionEntryImpl();

    out.setType(DatasetPermissionLevel.ENDUSER);

    return out;
  }

  static PermissionUtil getInstance() {
    if (instance == null)
      instance = new PermissionUtil();

    return instance;
  }
}
