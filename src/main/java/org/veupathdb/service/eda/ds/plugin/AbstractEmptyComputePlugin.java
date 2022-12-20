package org.veupathdb.service.eda.ds.plugin;

import org.veupathdb.service.eda.generated.model.VisualizationRequestBase;

/**
 * Simple abstract extension of AbstractPlugin that takes care of the fact that pass app viz plugins
 * don't have a compute.
 *
 * @param <T> type of request (must extend VisualizationRequestBase)
 * @param <S> plugin's spec class (must be or extend the generated spec class for this plugin)
 */
public abstract class AbstractEmptyComputePlugin<T extends VisualizationRequestBase, S> extends AbstractPlugin<T, S, EmptyComputeConfig> {

  @Override
  protected Class<EmptyComputeConfig> getComputeConfigClass() {
    return EmptyComputeConfig.class;
  }

  @Override
  protected boolean includeComputedVarsInStream() {
    return false;
  }

}
