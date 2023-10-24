package org.veupathdb.service.eda.compute.plugins

import org.veupathdb.service.eda.common.model.ReferenceMetadata
import java.util.function.Supplier

/**
 * Default implementation of [PluginConfigValidator] that performs no
 * validation.
 *
 * This should only be used as a fallback for plugins that do not require any
 * specific data validation on input configs.
 *
 * @author Elizabeth Paige Harper - https://github.com/foxcapades
 * @since 1.0.0
 */
class NoopPluginConfigValidator : PluginConfigValidator<Any> {

  override fun validate(request: Any, referenceMetadata : Supplier<ReferenceMetadata>) {
    // This method does nothing.
  }
}
