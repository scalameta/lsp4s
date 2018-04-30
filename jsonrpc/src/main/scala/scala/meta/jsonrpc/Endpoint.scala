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
 * @tparam A the request type for value inside "params" field.
 * @tparam B the response type for value inside "result" field.
 *           Is Unit in case of notifications.
 */
class Endpoint[A: Encoder: Decoder, B: Encoder: Decoder](val method: String) {

  def encoderParams: Encoder[A] = Encoder[A]
  def decoderParams: Decoder[A] = Decoder[A]

  def encoderResult: Encoder[B] = Encoder[B]
  def decoderResult: Decoder[B] = Decoder[B]

  /** Initiate request to be responded by client. */
  def request(request: A)(
      implicit client: Client
  ): Task[Either[Response.Error, B]] =
    client.request[A, B](method, request)

  /** Send notification request to be handled by client without response. */
  def notify(notification: A)(
      implicit client: Client
  ): Future[Ack] =
    client.notify[A](method, notification)

}

object Endpoint {

  def request[A: Encoder: Decoder, B: Encoder: Decoder](
      method: String
  ): Endpoint[A, B] =
    new Endpoint(method)

  def notification[A: Encoder: Decoder](method: String): Endpoint[A, Unit] =
    new Endpoint(method)

}
