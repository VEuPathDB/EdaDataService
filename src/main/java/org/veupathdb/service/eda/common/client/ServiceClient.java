package org.veupathdb.service.eda.common.client;

import java.util.Map;
import java.util.Map.Entry;

public abstract class ServiceClient {

  private final String _serviceBaseUrl;
  private final Entry<String, String> _authHeader;

  protected ServiceClient(String serviceBaseUrl, Entry<String, String> authHeader) {
    // remove trailing slash from baseUrl (paths must begin with a slash)
    _serviceBaseUrl = !serviceBaseUrl.endsWith("/") ? serviceBaseUrl :
        serviceBaseUrl.substring(0, serviceBaseUrl.length() - 1);
    _authHeader = authHeader;
  }

  protected String getUrl(String urlPath) {
    return _serviceBaseUrl + (urlPath.startsWith("/") ? urlPath : urlPath.substring(1));
  }

  protected Map<String, String> getAuthHeaderMap() {
    return Map.ofEntries(_authHeader);
  }

}
