package org.veupathdb.service.eda.ds.metadata;

import java.util.Arrays;
import org.veupathdb.service.eda.generated.model.AppOverview;
import org.veupathdb.service.eda.generated.model.AppOverviewImpl;
import org.veupathdb.service.eda.generated.model.AppsGetResponse;
import org.veupathdb.service.eda.generated.model.AppsGetResponseImpl;
import org.veupathdb.service.eda.generated.model.VisualizationOverview;
import org.veupathdb.service.eda.generated.model.VisualizationOverviewImpl;

public class AppsMetadata {

  // NOTE: these names must match the url segments defined in the api.raml
  public static final AppsGetResponse APPS = apps(
      app("pass", null, null,
          viz("map-markers", null, null),
          viz("scatterplot", "Scatter plot", "Visualize the relationship between two continuous variables."),
          viz("densityplot", "Density plot", "Visualize kernel density estimates for a continuous variable."),
          viz("lineplot", "Line plot", "Visualize the relationship between two continuous variables."),
          viz("date-histogram-num-bins", "Histogram", "Visualize the distribution of a continuous variable."),
          viz("date-histogram-bin-width", "Histogram", "Visualize the distribution of a continuous variable."),
          viz("numeric-histogram-num-bins", "Histogram", "Visualize the distribution of a continuous variable."),
          viz("numeric-histogram-bin-width", "Histogram", "Visualize the distribution of a continuous variable."),
          viz("barplot", "Bar plot", "Visualize the distribution of a categorical variable."),
          viz("boxplot", "Box plot", "Visualize summary values for a continuous variable."),
          viz("heatmap", "Heatmap", "Visualize the magnitude of a continuous numeric variable."),
          viz("conttable", "RxC Contingency Table", "Visualize the frequency distribution and chi sq test results for two categorical variables."),
          viz("twobytwo", "2x2 Contingency Table", "Visualize the frequency distribution, relative risk and odds ratio for two dichotomous variables.")),
      app("sample", "Sample", "Wrapper app for sample/test plugins",
          viz("record-count", "Record Count",
              "Counts how many rows in a single stream of records"),
          viz("multi-stream", "Multiple Stream Combiner",
              "Merges streams for up to three vars into a single tabular output and dumps it"))
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
