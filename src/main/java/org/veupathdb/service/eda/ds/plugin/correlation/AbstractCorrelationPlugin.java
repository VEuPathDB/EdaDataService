package org.veupathdb.service.eda.ds.plugin.correlation;

import org.veupathdb.service.eda.ds.core.AbstractPlugin;

/**
 * Simple abstract extension of AbstractPlugin that takes care of the fact that all correlation
 * application visualization plugins have a CorrelationComputeConfig.
 *
 * @param <T> type of request (must extend DataPluginRequestBase)
 * @param <S> plugin's spec class (must be or extend the generated spec class for this plugin)
 */
public class AbstractCorrelationPlugin<T extends DataPluginRequestBase, S> extends AbstractPlugin<T, S, CorrelationComputeConfig> {

  protected class CorrelationComputeClassGroup extends ClassGroup {
    public CorrelationComputeClassGroup(Class<T> visualizationRequestClass, Class<S> visualizationSpecClass) {
      super(visualizationRequestClass, visualizationSpecClass, CorrelationComputeConfig.class);
    }
  }

  @Override
  public boolean computeGeneratesVars() {
    return false;
  }
}
