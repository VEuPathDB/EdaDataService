package org.veupathdb.service.eda.ds.metadata;

import java.util.Arrays;
import java.util.List;

import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec;
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
import org.veupathdb.service.eda.ds.plugin.pass.MapMarkersOverlayPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.ScatterplotPlugin;
import org.veupathdb.service.eda.ds.plugin.pass.TwoByTwoPlugin;
import org.veupathdb.service.eda.ds.plugin.sample.ExampleComputeVizPlugin;
import org.veupathdb.service.eda.ds.plugin.sample.MultiStreamPlugin;
import org.veupathdb.service.eda.ds.plugin.sample.RecordCountPlugin;
import org.veupathdb.service.eda.ds.plugin.sample.TestCollectionPlugin;
import org.veupathdb.service.eda.generated.model.*;

public class AppsMetadata {

  public static final String CLINEPI_PROJECT = "ClinEpiDB";
  public static final String ALLCLINEPI_PROJECT = "AllClinEpiDB";
  public static final String MICROBIOME_PROJECT = "MicrobiomeDB";
  public static final String VECTORBASE_PROJECT = "VectorBase";

  // NOTE: these names must match the url segments defined in the api.raml
  // Pass vizs are now different based on mbio vs clinepi so we need to adjust the below array?
  public static final AppsGetResponse APPS = apps(
      app("standalone-map", "Standalone Map", null,
          "A collection of visualizations designed to support the unbiased exploration of relationships between spatiotemporal variables in a cartographic map",
          Arrays.asList(CLINEPI_PROJECT, ALLCLINEPI_PROJECT, VECTORBASE_PROJECT),
          viz("histogram", new FloatingHistogramPlugin()),
          viz("barplot", new FloatingBarplotPlugin()),
          viz("scatterplot", new FloatingScatterplotPlugin()),
          viz("boxplot", new FloatingBoxplotPlugin()),
          viz("twobytwo", new FloatingTwoByTwoPlugin()),
          viz("conttable", new FloatingContTablePlugin()),
          viz("lineplot", new FloatingLineplotPlugin()),
          viz("map-markers", new StandaloneMapMarkersPlugin())),
      app("pass", "Pass-Through", null,
          "A collection of visualizations designed to support the unbiased exploration of relationships between variables",
          Arrays.asList(CLINEPI_PROJECT, ALLCLINEPI_PROJECT, VECTORBASE_PROJECT),
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
          viz("map-markers-overlay", new MapMarkersOverlayPlugin())),
      app("alphadiv", "Alpha Diversity", "alphadiv",
          "Visualize within-sample (alpha) microbial diversity based on field-standard metrics, such as the Shannon Diversity Index, Simpson's Diversity Index, and Pielou's Evenness.",
          List.of(MICROBIOME_PROJECT),
          viz("boxplot", new AlphaDivBoxplotPlugin()),
          viz("scatterplot", new AlphaDivScatterplotPlugin())),
      app("abundance", "Ranked Abundance", "rankedabundance",
          "Plot the top n taxa, pathways, or genes from any study, ranked by either the median, maximum, variance, or third quartile of the relative abundance values.",
          List.of(MICROBIOME_PROJECT),
          viz("boxplot", new AbundanceBoxplotPlugin()),
          viz("scatterplot", new AbundanceScatterplotPlugin())),
      app("betadiv", "Beta Diversity", "betadiv",
          "Visualize between-sample (beta) comparisons in microbial diversity, using Bray-Curtis dissimilarity, Jensen-Shannon Divergence, or the Jaccard Index",
          List.of(MICROBIOME_PROJECT),
          viz("scatterplot", new BetaDivScatterplotPlugin())),
      app("distributions", "Distributions", null,
          "Plot simple distributions for any continuous variable, including metadata (e.g. age, height, etc.) or microbial assay results.",
          List.of(MICROBIOME_PROJECT),
          viz("histogram", new HistogramPlugin()),
          viz("boxplot", new BoxplotPlugin())),
      app("countsandproportions", "Counts and Proportions", null,
          "Use standard bar plots and 'row by column' (RxC) or 2x2 contingency tables to examine and compare frequencies in the data.",
          List.of(MICROBIOME_PROJECT),
          viz("barplot", new BarplotPlugin()),
          viz("twobytwo", new TwoByTwoPlugin()),
          viz("conttable", new ContTablePlugin())),
      app("xyrelationships", "X-Y Relationships", null,
          "Interested in creating your own X-Y visualizations of any study variables?  Look no further!  Click on one of the plot types on the right and get ready to be creative.",
          List.of(MICROBIOME_PROJECT),
          viz("scatterplot", new ScatterplotPlugin()),
          viz("lineplot", new LineplotPlugin())),
      app("sample", "Sample", null,
          "Wrapper app for sample/test plugins",
          List.of(),
          viz("record-count", new RecordCountPlugin()),
          viz("multi-stream", new MultiStreamPlugin()),
          viz("collections-test", new TestCollectionPlugin())),
      app("samplewithcompute", "Sample With Compute", "example",
          "Wrapper app for sample/test plugins that have associated computes",
          List.of(),
          viz("viz-with-compute", new ExampleComputeVizPlugin()))
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

  private static <T extends VisualizationRequestBase, S, R> VisualizationOverview viz(String urlSegment, AbstractPlugin<T, S, R> visualizationPlugin) {
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
