package org.veupathdb.service.eda.compute.plugins.example;

import org.jetbrains.annotations.NotNull;
import org.veupathdb.service.eda.compute.plugins.PluginConfigValidator;
import org.veupathdb.service.eda.compute.plugins.PluginContext;
import org.veupathdb.service.eda.compute.plugins.PluginProvider;
import org.veupathdb.service.eda.compute.plugins.PluginQueue;
import org.veupathdb.service.eda.generated.model.ExampleComputeConfig;
import org.veupathdb.service.eda.generated.model.ExamplePluginRequest;
import org.veupathdb.service.eda.generated.model.ExamplePluginRequestImpl;

public class ExamplePluginProvider implements PluginProvider<ExamplePluginRequest, ExampleComputeConfig> {
  @NotNull
  @Override
  public String getUrlSegment() {
    return "example";
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "Example Plugin";
  }

  @NotNull
  @Override
  public PluginQueue getTargetQueue() {
    return PluginQueue.Fast;
  }

  @NotNull
  @Override
  public Class<? extends ExamplePluginRequest> getRequestClass() {
    return ExamplePluginRequestImpl.class;
  }

  @NotNull
  @Override
  public PluginConfigValidator<ExamplePluginRequest> getValidator() {
    return new ExamplePluginInputValidator();
  }

  @NotNull
  @Override
  public ExamplePlugin createPlugin(@NotNull PluginContext<ExamplePluginRequest, ExampleComputeConfig> context) {
    return new ExamplePlugin(context);
  }
}
