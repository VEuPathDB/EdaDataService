package org.veupathdb.service.eda.access.service.permissions;

import org.veupathdb.service.eda.generated.model.DatasetPermissionEntry;
import org.veupathdb.service.eda.generated.model.PermissionsGetResponse;

import java.util.HashMap;

public class PermissionMap
  extends HashMap<String, DatasetPermissionEntry>
  implements PermissionsGetResponse.PerDatasetType
{
}
