package org.veupathdb.service.eda.ds.plugin;

import java.util.Arrays;
import org.veupathdb.service.eda.generated.model.AppOverview;
import org.veupathdb.service.eda.generated.model.AppOverviewImpl;
import org.veupathdb.service.eda.generated.model.AppsGetResponse;
import org.veupathdb.service.eda.generated.model.AppsGetResponseImpl;
import org.veupathdb.service.eda.generated.model.VisualizationOverview;
import org.veupathdb.service.eda.generated.model.VisualizationOverviewImpl;

public class PluginMetadata {

  // NOTE: these names must match the url segments defined in the api.raml
  public static final AppsGetResponse APPS = apps(
      app("pass", null, null,
          viz("record-count", null, null),
          viz("map-markers", null, null),
          viz("scatterplot", null, null),
          viz("date-histogram-num-bins", null, null),
          viz("date-histogram-bin-width", null, null),
          viz("numeric-histogram-num-bins", null, null),
          viz("numeric-histogram-bin-width", null, null),
          viz("barplot", null, null),
          viz("boxplot", null, null),
          viz("heatmap", null, null),
          viz("mosaic", null, null)
      )
  );

  //******************************************
  //***  Helper functions
  //******************************************

  private static AppsGetResponse apps(AppOverview... apps) {
    AppsGetResponse responseObj = new AppsGetResponseImpl();
    responseObj.setApps(Arrays.asList(apps));
    return responseObj;
  }

  private static AppOverview app(String name, String displayName, String description, VisualizationOverview... visualizations) {
    AppOverviewImpl app = new AppOverviewImpl();
    app.setName(name);
    app.setDisplayName(displayName);
    app.setDescription(description);
    app.setVisualizations(Arrays.asList(visualizations));
    return app;
  }

  private static VisualizationOverview viz(String name, String displayName, String description) {
    VisualizationOverviewImpl viz = new VisualizationOverviewImpl();
    viz.setName(name);
    viz.setDisplayName(displayName);
    viz.setDescription(description);
    return viz;
  }
}
