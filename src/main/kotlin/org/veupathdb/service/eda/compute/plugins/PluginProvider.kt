package org.veupathdb.service.eda.compute.plugins

import org.veupathdb.service.eda.common.plugin.constraint.ConstraintSpec
import org.veupathdb.service.eda.generated.model.ComputeRequestBase

/**
 * Plugin Provider.
 *
 * Helper class that describes and assists in instantiating an instance of an
 * [AbstractPlugin] implementation.
 *
 * @param R HTTP request payload type.
 *
 * @param C Plugin configuration type.
 *
 * @author Elizabeth Paige Harper - https://github.com/foxcapades
 * @since 1.0.0
 */
interface PluginProvider<R : ComputeRequestBase, C> : PluginMeta<R> {

  /**
   * Creates a new instance of this plugin
   */
  fun createPlugin(context: PluginContext<R, C>): AbstractPlugin<R, C>

  /**
   * Plugin Request Configuration Validator
   *
   * The config validator is used before the plugin job is queued to ensure the
   * incoming request is sane.
   *
   * A default implementation of this method is provided that returns a no-op
   * config validator.
   *
   * Plugins may choose to implement this method and use their own config
   * validators
   */
  @Suppress("UNCHECKED_CAST")
  fun getValidator(): PluginConfigValidator<R> =
    NoopPluginConfigValidator() as PluginConfigValidator<R>

  /**
   * Plugin Context Builder
   */
  fun getContextBuilder(): PluginContextBuilder<R, C> =
    PluginContextBuilder()

  /**
   * Override this method to add variable and collection constraints to your plugin's input.
   */
  fun getConstraintSpec(): ConstraintSpec = ConstraintSpec()

}
