package scala.meta.jsonrpc

import io.circe.Decoder
import io.circe.Encoder
import monix.eval.Task
import monix.execution.Ack
import scala.concurrent.Future

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
class Endpoint[Params: Encoder: Decoder, Result: Encoder: Decoder](
    val method: String
) {

  def encoderParams: Encoder[Params] = Encoder[Params]
  def encoderResult: Encoder[Result] = Encoder[Result]

  def decoderParams: Decoder[Params] = Decoder[Params]
  def decoderResult: Decoder[Result] = Decoder[Result]

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

  def request[A: Encoder: Decoder, B: Encoder: Decoder](
      method: String
  ): Endpoint[A, B] =
    new Endpoint(method)

  def notification[A: Encoder: Decoder](method: String): Endpoint[A, Unit] =
    new Endpoint(method)

}
