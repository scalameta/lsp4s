package scala.meta.jsonrpc

import monix.execution.Cancelable
import monix.execution.CancelableFuture
import monix.execution.Scheduler
import scribe.Logger
import scribe.LoggerSupport

/**
 * A connection with another JSON-RPC entity.
 *
 * @param client used to send requests/notification to the other entity.
 * @param server server on this side listening to input streams from the other entity.
 */
final case class Connection(
    client: LanguageClient,
    server: CancelableFuture[Unit]
) extends Cancelable {
  override def cancel(): Unit = server.cancel()
}

object Connection {

  def simple(io: InputOutput, name: String)(
      f: LanguageClient => Services
  )(implicit s: Scheduler): Connection =
    Connection(
      io,
      Logger(s"$name-server"),
      Logger(s"$name-client")
    )(f)

  def apply(
      io: InputOutput,
      serverLogger: LoggerSupport,
      clientLogger: LoggerSupport
  )(
      f: LanguageClient => Services
  )(implicit s: Scheduler): Connection = {
    val messages =
      BaseProtocolMessage.fromInputStream(io.in, serverLogger)
    val client =
      LanguageClient.fromOutputStream(io.out, clientLogger)
    val server =
      new LanguageServer(messages, client, f(client), s, serverLogger)
    Connection(client, server.startTask.executeWithFork.runAsync)
  }

}
