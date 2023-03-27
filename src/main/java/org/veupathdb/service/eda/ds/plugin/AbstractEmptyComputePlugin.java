package org.veupathdb.service.eda.ds.plugin;

import org.veupathdb.service.eda.generated.model.DataPluginRequestBase;

/**
 * Simple abstract extension of AbstractPlugin that takes care of the fact that pass app viz plugins
 * don't have a compute.
 *
 * @param <T> type of request (must extend DataPluginRequestBase)
 * @param <S> plugin's spec class (must be or extend the generated spec class for this plugin)
 */
public abstract class AbstractEmptyComputePlugin<T extends DataPluginRequestBase, S> extends AbstractPlugin<T, S, Void> {

  protected class EmptyComputeClassGroup extends ClassGroup {
    public EmptyComputeClassGroup(Class<T> visualizationRequestClass, Class<S> visualizationSpecClass) {
      super(visualizationRequestClass, visualizationSpecClass, Void.class);
    }
  }

}
