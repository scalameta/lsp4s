package scala.meta.jsonrpc

import monix.eval.Task
import monix.execution.Ack
import scala.concurrent.Future
import scala.meta.jsonrpc.pickle.ReadWriter

/**
 * A single JSON-RPC method with it's request and response types.
 *
 * A protocol like LSP is in essence a collection of endpoints.
 * An endpoint is traditionally implemented like this:
 *
 * {{{
 *   // Request
 *   object Definition extends Endpoint[DefinitionParams, DefinitionResult]("textDocument/definition")
 *   // Notification
 *   object Log extends Endpoint[LogParams, Unit]("window/log")
 * }}}
 *
 * An endpoint can be used to send requests assuming an implicit client is in scope:
 *
 * {{{
 *   Definition.request(DefinitionParams(...)): Task[...]
 *   Log.notify(LogParams(...)): Unit
 * }}}
 *
 * An endpoint can be used to implement a request handler:
 *
 * {{{
 *   val services = Services.empty(logger)
 *     .request(Definition) { params =>
 *       DefinitionResult(...)
 *     }
 *     .notification(Log) { params =>
 *       // handle params with no response
 *     }
 * }}}
 *
 * @param method the name of this endpoint for value inside "method" field.
 * @tparam Params the request type for value inside "params" field.
 * @tparam Result the response type for value inside "result" field.
 *                Is Unit in case of notifications.
 */
class Endpoint[Params: ReadWriter, Result: ReadWriter](val method: String) {

  /** JSON encoder/decoder for the Params type. */
  def readwriterParams: ReadWriter[Params] = implicitly[ReadWriter[Params]]

  /** JSON encoder/decoder for the Result type. */
  def readwriterResult: ReadWriter[Result] = implicitly[ReadWriter[Result]]

  /** Initiate request to be responded by client. */
  def request(request: Params)(
      implicit client: Client
  ): Task[Either[Response.Error, Result]] =
    client.request[Params, Result](method, request)

  /** Send notification request to be handled by client without response. */
  def notify(notification: Params)(
      implicit client: Client
  ): Future[Ack] =
    client.notify[Params](method, notification)

}

object Endpoint {

  def request[A: ReadWriter, B: ReadWriter](
      method: String
  ): Endpoint[A, B] =
    new Endpoint(method)

  def notification[A: ReadWriter](method: String): Endpoint[A, Unit] =
    new Endpoint(method)

}
