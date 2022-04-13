package org.veupathdb.service.eda.ds.metadata;

import java.util.Arrays;
import java.util.List;
import org.veupathdb.service.eda.ds.constraints.ConstraintSpec;
import org.veupathdb.service.eda.ds.plugin.AbstractPlugin;
import org.veupathdb.service.eda.ds.plugin.alphadiv.AlphaDivBoxplotPlugin;
import org.veupathdb.service.eda.ds.plugin.alphadiv.AlphaDivScatterplotPlugin;
import org.veupathdb.service.eda.ds.plugin.abundance.AbundanceBoxplotPlugin;
import org.veupathdb.service.eda.ds.plugin.abundance.AbundanceScatterplotPlugin;
import org.veupathdb.service.eda.ds.plugin.betadiv.BetaDivScatterplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.BarplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.BoxplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.ContTablePlugin;
import org.veupathdb.service.eda.ds.plugin.pass.DensityplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.HeatmapPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.HistogramPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.LineplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.MapPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.PieplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.ScatterplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.TimeSeriesPlugin;
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

  public static final String CLINEPI_PROJECT = "ClinEpiDB";
  public static final String ALLCLINEPI_PROJECT = "AllClinEpiDB";
  public static final String MICROBIOME_PROJECT = "MicrobiomeDB";

  // NOTE: these names must match the url segments defined in the api.raml
  // Pass vizs are now different based on mbio vs clinepi so we need to adjust the below array?
  public static final AppsGetResponse APPS = apps(
      app("pass", "Pass-Through", null,
          "A collection of visualizations designed to support the unbiased exploration of relationships between variables",
          Arrays.asList(CLINEPI_PROJECT, ALLCLINEPI_PROJECT),
          viz("histogram", new HistogramPlugin()),
          viz("barplot", new BarplotPlugin()),
          viz("scatterplot", new ScatterplotPlugin()),
          viz("boxplot", new BoxplotPlugin()),
          viz("twobytwo", new TwoByTwoPlugin()),
          viz("conttable", new ContTablePlugin()),
          viz("lineplot", new LineplotPlugin()),
          viz("densityplot", new DensityplotPlugin()),
          viz("heatmap", new HeatmapPlugin()),
          viz("map-markers", new MapPlugin()),
          viz("pieplot", new PieplotPlugin())),
      app("alphadiv", "Alpha Diversity", "AlphaDivComputation",
          "A collection of visualizations designed to support the unbiased exploration of relationships between variables and Alpha Diversity.",
          List.of(MICROBIOME_PROJECT),
          viz("boxplot", new AlphaDivBoxplotPlugin()),
          viz("scatterplot", new AlphaDivScatterplotPlugin())),
      app("abundance", "Ranked Abundance", "RankedAbundanceComputation",
          "A collection of visualizations designed to support the unbiased exploration of relationships between variables and abundance data.",
          List.of(MICROBIOME_PROJECT),
          viz("boxplot", new AbundanceBoxplotPlugin()),
          viz("scatterplot", new AbundanceScatterplotPlugin())),
      app("betadiv", "Beta Diversity", "BetaDivComputation",
          "A collection of visualizations designed to support the unbiased exploration of relationships between variables and Beta Diversity.",
          List.of(MICROBIOME_PROJECT),
          viz("scatterplot", new BetaDivScatterplotPlugin())),
      app("distributions", "Distributions", "null",
          "A collection of visualizations designed to support the unbiased exploration of data distributions.",
          List.of(MICROBIOME_PROJECT),
          viz("boxplot", new BoxplotPlugin()),
          viz("histogram", new HistogramPlugin())),
      app("countsandproportions", "Counts and Proportions", "null",
          "A collection of visualizations designed to support the unbiased exploration of quantities and relative sizes of data.",
          List.of(MICROBIOME_PROJECT),
          viz("twobytwo", new TwoByTwoPlugin()),
          viz("conttable", new ContTablePlugin()),
          viz("barplot", new BarplotPlugin())),
      app("xyrelationships", "X-Y Relationships", "null",
          "A collection of visualizations designed to support the unbiased exploration of relationships between two continuous variables.",
          List.of(MICROBIOME_PROJECT),
          viz("scatterplot", new ScatterplotPlugin()),
          viz("lineplot", new LineplotPlugin())),
      app("sample", "Sample", "Wrapper app for sample/test plugins", null,
          List.of(),
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

  private static AppOverview app(String name, String displayName, String computeName, String description, List<String> projects, VisualizationOverview... visualizations) {
    AppOverviewImpl app = new AppOverviewImpl();
    app.setName(name);
    app.setDisplayName(displayName);
    app.setDescription(description);
    app.setProjects(projects);
    app.setComputeName(computeName);
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
