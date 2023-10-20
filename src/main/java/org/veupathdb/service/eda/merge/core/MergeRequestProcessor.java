package org.veupathdb.service.eda.merge.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.client.ResponseFuture;
import org.gusdb.fgputil.functional.FunctionalInterfaces.ConsumerWithException;
import org.gusdb.fgputil.functional.Functions;
import org.gusdb.fgputil.iterator.CloseableIterator;
import org.gusdb.fgputil.iterator.IteratorUtil;
import org.gusdb.fgputil.validation.ValidationException;
import org.veupathdb.service.eda.Resources;
import org.veupathdb.service.eda.common.client.StreamingDataClient;
import org.veupathdb.service.eda.common.client.spec.StreamSpec;
import org.veupathdb.service.eda.common.model.EntityDef;
import org.veupathdb.service.eda.common.model.ReferenceMetadata;
import org.veupathdb.service.eda.common.model.VariableDef;
import org.veupathdb.service.eda.generated.model.VariableSpec;
import org.veupathdb.service.eda.merge.core.request.ComputeInfo;
import org.veupathdb.service.eda.merge.core.request.MergedTabularRequestResources;
import org.veupathdb.service.eda.merge.core.stream.RootStreamingEntityNode;
import org.veupathdb.service.eda.ss.model.Entity;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.db.FilteredResultFactory;
import org.veupathdb.service.eda.ss.model.variable.VariableWithValues;
import org.veupathdb.service.eda.subset.service.ApiConversionUtil;
import org.veupathdb.service.eda.subset.service.StudiesService;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.gusdb.fgputil.FormatUtil.TAB;
import static org.veupathdb.service.eda.merge.core.stream.RootStreamingEntityNode.COMPUTED_VAR_STREAM_NAME;


/**
 * Top-level tabular request processing class, responsible for (in execution order):
 *
 * 1. initializing and collecting metadata
 * 2. building an entity stream processing tree which will merge all incoming data streams
 * 3. collecting stream specs for required streams
 * 4. requesting streams from subsetting and compute services
 * 5. determining whether a single required stream can be directly passed out as the response (with no merge processing)
 * 6. distributing the incoming data streams to the entity stream processing tree
 * 7. returning a consumer of the output stream which writes the merged streams
 *
 */
public class MergeRequestProcessor {

  private static final Logger LOG = LogManager.getLogger(MergeRequestProcessor.class);
  private final MergedTabularRequestResources _resources;

  public MergeRequestProcessor(MergedTabularRequestResources resources) {
    _resources = resources;
  }

  public Consumer<OutputStream> createMergedResponseSupplier() throws ValidationException {

    // gather request resources
    String targetEntityId = _resources.getTargetEntityId();
    List<VariableSpec> outputVarSpecs = _resources.getOutputVariableSpecs();
    ReferenceMetadata metadata = _resources.getMetadata();
    Optional<ComputeInfo> computeInfo = _resources.getComputeInfo();
    Study study = StudiesService.getStudyResolver().getStudyById(_resources.getMetadata().getStudyId());

    // request validated; convert requested entity and vars to defs
    EntityDef targetEntity = metadata.getEntity(targetEntityId).orElseThrow();
    List<VariableDef> outputVarDefs = metadata.getTabularColumns(targetEntity, outputVarSpecs);
    List<VariableSpec> outputVars = new ArrayList<>(outputVarDefs.stream().map(v -> (VariableSpec)v).toList());

    // build entity node tree to aggregate the data into a streaming response
    RootStreamingEntityNode targetStream = new RootStreamingEntityNode(targetEntity, outputVarDefs,
        _resources.getSubsetFilters(), metadata, _resources.getDerivedVariableFactory(), computeInfo);
    LOG.info("Created the following entity node tree: " + targetStream);

    // get stream specs for streams needed by the node tree, which will be merged into this request's response
    Map<String, StreamSpec> requiredStreams = Functions.getMapFromValues(targetStream.getRequiredStreamSpecs(), StreamSpec::getStreamName);
    Entity entity = study.getEntity(_resources.getTargetEntityId()).orElseThrow();
    List<VariableWithValues> variables = entity.getVariables().stream()
        .filter(var -> _resources.getOutputVariableSpecs().stream().anyMatch(outputVar -> var.getId().equals(outputVar.getVariableId())))
        .map(var -> (VariableWithValues) var).toList();

    // create stream generator
    Function<StreamSpec, CloseableIterator<Map<String, String>>> streamGenerator = spec ->
        COMPUTED_VAR_STREAM_NAME.equals(spec.getStreamName())
            // need to get compute stream from compute service
            ? _resources.getInMemoryComputeStream()
            // all other streams come from subsetting service
            : FilteredResultFactory.produceUnformattedTabularSubset(study,
            study.getEntity(spec.getEntityId()).orElseThrow(),
            spec.stream()
                .map(varSpec -> study.getEntity(spec.getEntityId()).orElseThrow().getVariableOrThrow(varSpec.getVariableId()))
                .map(var -> (VariableWithValues) var)
                .collect(Collectors.toList()),
            ApiConversionUtil.toInternalFilters(study, spec.getFiltersOverride().orElse(_resources.getSubsetFilters()), Resources.getAppDbSchema()), // Move this up?
            Resources.getBinaryValuesStreamer(), false, Resources.getApplicationDataSource(), Resources.getAppDbSchema());
    return out -> {

      // create stream processor
      ConsumerWithException<Map<String,CloseableIterator<Map<String, String>>>> streamProcessor =
          dataStreams -> writeMergedStream(targetStream, dataStreams, out);

      // build and process streams
      StreamingDataClient.buildAndProcessInMemoryStreams(new ArrayList<>(requiredStreams.values()), streamGenerator, streamProcessor);
    };
  }

  private static void writePassThroughStream(List<VariableSpec> outputVars, InputStream in, OutputStream out) {
    try (BufferedInputStream is = new BufferedInputStream(in);
         BufferedOutputStream os = new BufferedOutputStream(out)) {
      do {
        // Skip over header line to re-write with dot notation.
      } while (is.read() != '\n');
      String headerRow = String.join(TAB, VariableDef.toDotNotation(outputVars));
      os.write(headerRow.getBytes(StandardCharsets.UTF_8));
      os.write('\n');

      LOG.info("Transferring subsetting stream to output since there is only one stream.");
      is.transferTo(os);
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to write output stream", e);
    }
  }

  private static void writeMergedStream(RootStreamingEntityNode targetEntityStream, Map<String, CloseableIterator<Map<String, String>>> dataStreams, OutputStream out) {

    LOG.info("All requested streams (" + dataStreams.size() + ") ready for consumption");

    // distribute the streams to their processors and make sure they all get claimed
    Map<String, CloseableIterator<Map<String, String>>> distributionMap = new HashMap<>(dataStreams); // make a copy which will get cleared out
    targetEntityStream.acceptDataStreams(distributionMap);
    if (!distributionMap.isEmpty())
      throw new IllegalStateException("Not all requested data streams were claimed by the processor tree.  " +
          "Remaining: " + String.join(", ", distributionMap.keySet()));

    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {

      // write the header row
      String headerRow = String.join(TAB, targetEntityStream.getOrderedOutputVars());
      LOG.info("Writing header row:" + headerRow);
      writer.write(headerRow);
      writer.newLine();

      // write the entity rows
      for (Map<String,String> row : IteratorUtil.toIterable(targetEntityStream)) {
        writer.write(String.join(TAB, row.values()));
        writer.newLine();
      }

      // flush any remaining chars
      writer.flush();
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to write output stream", e);
    }
  }
}
