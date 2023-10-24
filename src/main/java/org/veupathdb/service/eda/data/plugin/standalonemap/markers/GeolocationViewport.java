package org.veupathdb.service.eda.data.plugin.standalonemap.markers;

public class GeolocationViewport {
  private double xMin;
  private double xMax;
  private double yMin;
  private double yMax;
  private boolean viewportIncludesIntlDateLine;

  public GeolocationViewport(double xMin, double xMax,
                             double yMin, double yMax) {
    this.xMin = xMin;
    this.xMax = xMax;
    this.yMin = yMin;
    this.yMax = yMax;
    this.viewportIncludesIntlDateLine = yMin > yMax;
  }


  public static GeolocationViewport fromApiViewport(org.veupathdb.service.eda.generated.model.GeolocationViewport viewport) {
    return new GeolocationViewport(
        Double.parseDouble(viewport.getLatitude().getXMin()),
        Double.parseDouble(viewport.getLatitude().getXMax()),
        viewport.getLongitude().getLeft().doubleValue(),
        viewport.getLongitude().getRight().doubleValue()
    );
  }

  public Boolean containsCoordinates(double latitude, double longitude) {
    if (latitude < xMin || latitude > xMax) {
      return false;
    }
    if (viewportIncludesIntlDateLine) {
      if (longitude < yMin && longitude > yMax) {
        return false;
      }
    } else {
      if (longitude < yMin || longitude > yMax) {
        return false;
      }
    }
    return true;
  }
}
