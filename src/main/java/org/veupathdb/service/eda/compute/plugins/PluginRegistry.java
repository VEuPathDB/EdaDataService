package org.veupathdb.service.eda.compute.plugins;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.veupathdb.service.eda.compute.plugins.alphadiv.AlphaDivPluginProvider;
import org.veupathdb.service.eda.compute.plugins.betadiv.BetaDivPluginProvider;
import org.veupathdb.service.eda.compute.plugins.differentialabundance.DifferentialAbundancePluginProvider;
import org.veupathdb.service.eda.compute.plugins.example.ExamplePluginProvider;
import org.veupathdb.service.eda.compute.plugins.rankedabundance.RankedAbundancePluginProvider;
import org.veupathdb.service.eda.generated.model.ComputeRequestBase;
import org.veupathdb.service.eda.generated.model.PluginOverview;
import org.veupathdb.service.eda.generated.model.PluginOverviewImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plugin Registry
 * <p>
 * Singleton container class where all plugins are registered.
 * <p>
 * Provides methods for looking up plugins by url segment and getting an
 * overview of the registered plugins.
 *
 * @author Elizabeth Paige Harper - https://github.com/foxcapades
 * @since 1.0.0
 */
public final class PluginRegistry {

  private static final Logger Log = LogManager.getLogger(PluginRegistry.class);

  private static final Map<String, PluginProvider<?, ?>> Registry;

  static {
    //
    // Begin Plugin List
    //
    // Add new plugins here.
    //

    var pluginList = List.of(
      new ExamplePluginProvider(),
      new AlphaDivPluginProvider(),
      new BetaDivPluginProvider(),
      new RankedAbundancePluginProvider(),
      new DifferentialAbundancePluginProvider()
    );

    //
    // End Plugin List
    //

    Log.info("Registering {} plugins.", pluginList.size());
    Registry = new HashMap<>(pluginList.size());
    for (var plugin : pluginList)
      Registry.put(plugin.getUrlSegment(), plugin);
  }

  /**
   * Returns the {@link PluginProvider} registered with the given url segment.
   * <p>
   * The returned provider is cast to its most basic allowed generic types as
   * the real generic types are unknown and shouldn't actually matter at run
   * time.
   *
   * @param urlSegment URL segment of the plugin to return.
   *
   * @return The target {@code PluginProvider}.
   *
   * @throws IllegalArgumentException If the given url segment does not match
   * any currently registered plugin.
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public static PluginProvider<ComputeRequestBase, Object> get(String urlSegment) {
    Log.trace("looking up plugin {}", urlSegment);
    return (PluginProvider<ComputeRequestBase, Object>) Registry.get(urlSegment);
  }

  /**
   * Builds an overview of details about the plugins currently registered with
   * the service.
   *
   * @return A {@code List} of  {@link PluginOverview} objects describing the
   * plugins currently registered.
   */
  @NotNull
  public static List<PluginOverview> getPluginOverview() {
    Log.trace("building plugin overview list");

    var out = new ArrayList<PluginOverview>(Registry.size());

    for (var it : Registry.values()) {
      var impl = new PluginOverviewImpl();
      impl.setName(it.getUrlSegment());
      impl.setDisplayName(it.getDisplayName());
      impl.setDescription(it.getDescription());
      impl.setDataElementConstraints(it.getConstraintSpec().stream().toList());
      out.add(impl);
    }

    return out;
  }

  private PluginRegistry() {}
}
