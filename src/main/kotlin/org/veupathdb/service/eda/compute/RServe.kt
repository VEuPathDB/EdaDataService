package org.veupathdb.service.eda.compute

import org.gusdb.fgputil.functional.FunctionalInterfaces.ConsumerWithException
import org.rosuda.REngine.Rserve.RConnection
import org.veupathdb.service.eda.Main
import org.veupathdb.service.eda.common.plugin.util.RFileSetProcessor
import org.veupathdb.service.eda.common.plugin.util.RServeClient
import java.io.InputStream
import java.io.OutputStream

/**
 * Convenience wrapper over the [RServeClient] class that injects the RServe
 * URL into the method calls.
 *
 * @author Elizabeth Paige Harper - https://github.com/foxcapades
 * @since 1.0.0
 */
object RServe {

  @JvmStatic
  fun useRConnection(consumer: ConsumerWithException<RConnection>) =
    RServeClient.useRConnection(Main.config.rServeHost, consumer)

  @JvmStatic
  fun useRConnectionWithRemoteFiles(dataStreams: Map<String, InputStream>, consumer: ConsumerWithException<RConnection>) =
    RServeClient.useRConnectionWithRemoteFiles(Main.config.rServeHost, dataStreams, consumer)

  @JvmStatic
  fun useRConnectionWithProcessedRemoteFiles(filesProcessor: RFileSetProcessor, consumer: ConsumerWithException<RConnection>) =
    RServeClient.useRConnectionWithProcessedRemoteFiles(Main.config.rServeHost, filesProcessor, consumer)

  @JvmStatic
  fun streamResult(connection: RConnection, cmd: String, out: OutputStream) =
    RServeClient.streamResult(connection, cmd, out)
}