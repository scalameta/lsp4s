package scala.meta.jsonrpc

import monix.execution.Cancelable
import monix.execution.CancelableFuture
import monix.execution.Scheduler
import scribe.Logger
import scribe.LoggerSupport

case class Connection(
    client: JsonRpcClient,
    server: CancelableFuture[Unit]
) extends Cancelable {
  override def cancel(): Unit = server.cancel()
}

object Connection {
  def apply(io: InputOutput, name: String)(
      f: LanguageClient => Services
  )(implicit s: Scheduler): Connection =
    Connection(
      io,
      Logger.byName(s"$name-server"),
      Logger.byName(s"$name-client")
    )(f)
  def apply(
      io: InputOutput,
      serverLoggerSupport: LoggerSupport,
      clientLoggerSupport: LoggerSupport
  )(
      f: LanguageClient => Services
  )(implicit s: Scheduler): Connection = {
    val messages =
      BaseProtocolMessage.fromInputStream(io.in, serverLoggerSupport)
    val client =
      LanguageClient(io.out, clientLoggerSupport)
    val server =
      LanguageServer(messages, client, f(client), s, serverLoggerSupport)
    Connection(client, server.startTask.executeWithFork.runAsync)
  }
}
