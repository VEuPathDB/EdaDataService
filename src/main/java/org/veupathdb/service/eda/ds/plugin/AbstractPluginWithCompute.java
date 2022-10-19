package org.veupathdb.service.eda.ds.plugin;

import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.generated.model.ComputeConfig;
import org.veupathdb.service.eda.generated.model.VisualizationRequestBase;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 *
 * @param <T> type of the visualization request (generated from RAML)
 * @param <S> type of the
 * @param <R> type of the compute config (pulled from compute service RAML)
 */
public abstract class AbstractPluginWithCompute<T extends VisualizationRequestBase, S, R> extends AbstractPlugin<T, S> {

  // returns the compute-spec class this visualization expects
  protected abstract Class<R> getComputeSpecClass();

  // return true to include computed vars as part of the tabular stream
  //   for the entity under which they are computed.  If true, a runtime
  //   error will occur if no stream spec exists for that entity.
  protected abstract boolean includeComputedVarsInStream();



  @Override
  protected void writeResults(OutputStream out, Map dataStreams) throws IOException {

  }


}
