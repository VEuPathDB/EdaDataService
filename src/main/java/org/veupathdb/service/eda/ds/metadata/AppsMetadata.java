package org.veupathdb.service.eda.ds.metadata;

import java.util.Arrays;
import java.util.List;
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.BarplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.BoxplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.ContTablePlugin;
import org.veupathdb.service.eda.ds.plugin.pass.DensityplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.HeatmapPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.HistogramPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.LineplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.MapPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.ScatterplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.TwoByTwoPlugin;
import org.veupathdb.service.eda.ds.plugin.sample.MultiStreamPlugin;
import org.veupathdb.service.eda.ds.plugin.sample.RecordCountPlugin;
import org.veupathdb.service.eda.generated.model.AppOverview;
import org.veupathdb.service.eda.generated.model.AppOverviewImpl;
import org.veupathdb.service.eda.generated.model.AppsGetResponse;
import org.veupathdb.service.eda.generated.model.AppsGetResponseImpl;
import org.veupathdb.service.eda.generated.model.VisualizationOverview;
import org.veupathdb.service.eda.generated.model.VisualizationOverviewImpl;

public class AppsMetadata {

  // NOTE: these names must match the url segments defined in the api.raml
  public static final AppsGetResponse APPS = apps(
      app("pass", "Pass-Through", 
          "A collection of visualizations designed to support the unbiased exploration of relationships between variables",
          Arrays.asList("ClinEpiDB", "MicrobiomeDB"),
          viz("map-markers", new MapPlugin()),
          viz("scatterplot", new ScatterplotPlugin()),
          viz("densityplot", new DensityplotPlugin()),
          viz("lineplot", new LineplotPlugin()),
          viz("histogram", new HistogramPlugin()),
          viz("barplot", new BarplotPlugin()),
          viz("boxplot", new BoxplotPlugin()),
          viz("heatmap", new HeatmapPlugin()),
          viz("conttable", new ContTablePlugin()),
          viz("twobytwo", new TwoByTwoPlugin())),
      app("sample", "Sample", "Wrapper app for sample/test plugins",
          Arrays.asList(""),
          viz("record-count", new RecordCountPlugin()),
          viz("multi-stream", new MultiStreamPlugin()))
  );

  //******************************************
  //***  Helper functions
  //******************************************

  private static AppsGetResponse apps(AppOverview... apps) {
    AppsGetResponse responseObj = new AppsGetResponseImpl();
    responseObj.setApps(Arrays.asList(apps));
    return responseObj;
  }

  private static AppOverview app(String name, String displayName, String description, List<String> projects, VisualizationOverview... visualizations) {
    AppOverviewImpl app = new AppOverviewImpl();
    app.setName(name);
    app.setDisplayName(displayName);
    app.setDescription(description);
    app.setProjects(projects);
    app.setVisualizations(Arrays.asList(visualizations));
    return app;
  }

  private static VisualizationOverview viz(String urlSegment, AbstractPlugin<?,?> visualizationPlugin) {
    ConstraintSpec constraints = visualizationPlugin.getConstraintSpec();
    VisualizationOverviewImpl viz = new VisualizationOverviewImpl();
    viz.setName(urlSegment);
    viz.setDisplayName(visualizationPlugin.getDisplayName());
    viz.setDescription(visualizationPlugin.getDescription());
    viz.setProjects(visualizationPlugin.getProjects());
    viz.setMaxPanels(visualizationPlugin.getMaxPanels());
    viz.setDataElementConstraints(constraints.isEmpty() ? null : constraints);
    viz.setDataElementDependencyOrder(constraints.getDependencyOrder());
    return viz;
  }
}
