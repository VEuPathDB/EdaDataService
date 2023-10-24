package org.veupathdb.service.eda.compute.plugins

import org.veupathdb.lib.container.jaxrs.errors.UnprocessableEntityException
import org.veupathdb.service.eda.common.model.ReferenceMetadata
import java.util.function.Supplier

/**
 * Plugin Configuration Validator
 *
 * Defines a type that can be called to validate the contents of a given plugin
 * configuration value.
 *
 * @author Elizabeth Paige Harper - https://github.com/Foxcapades
 * @since 1.0.0
 */
@FunctionalInterface
fun interface PluginConfigValidator<C> {

  /**
   * Validates the given configuration.
   *
   * @throws UnprocessableEntityException if the given plugin configuration
   * fails the implementation defined validation.
   */
  fun validate(request: C, referenceMetadata : Supplier<ReferenceMetadata>)
}
