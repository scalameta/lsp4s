package scala.meta.jsonrpc

import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.syntax._
import monix.eval.Task
import scribe.LoggerSupport

/** Handler for a single endpoint. */
trait Service {

  /** Handle a request or notification */
  def handle(message: Message): Task[Response]

  /** The JSON-RPC method name this service handles */
  def methodName: String

}

object Service {

  /**
   * Build service for a request endpoint.
   *
   * @param method name of the method handled by this service.
   * @param f handler for this service.
   * @tparam A type inside "params" field of the request to be decoded.
   * @tparam B type inside "result" field of the response to be encoded.
   */
  def request[A: Encoder: Decoder, B: Encoder: Decoder](method: String)(
      f: A => Task[Either[Response.Error, B]]
  ): Service = new Service {
    override def methodName: String = method
    override def handle(message: Message): Task[Response] = message match {
      case Request(`method`, params, id) =>
        params.getOrElse(Json.Null).as[A] match {
          case Left(err) =>
            Task(Response.invalidParams(err.toString, id))
          case Right(value) =>
            f.apply(value).map {
              case Right(response) => Response.ok(response.asJson, id)
              // Service[A, ...] doesn't have access to the request ID so
              // by convention it's OK to set the ID to null by default
              // and we fill it in here instead.
              case Left(err) => err.copy(id = id)
            }
        }
      case Request(invalidMethod, _, id) =>
        Task(Response.methodNotFound(invalidMethod, id))
      case _ =>
        Task(Response.invalidRequest(s"Expected request, obtained $message"))
    }
  }

  /**
   * Build service for a notification endpoint.
   *
   * @param method name of the method handled by this service.
   * @param f handler for this service.
   * @tparam A type inside "params" field of the notification to be decoded.
   */
  def notification[A: Encoder: Decoder](method: String, logger: LoggerSupport)(
      f: A => Task[Unit]
  ): Service = new Service {
    override def methodName: String = method
    private def fail(msg: String): Task[Response] = Task {
      logger.error(msg)
      Response.empty
    }
    override def handle(message: Message): Task[Response] = message match {
      case Notification(`method`, params) =>
        params.getOrElse(Json.Null).as[A] match {
          case Left(err) =>
            fail(s"Failed to parse notification $message. Errors: $err")
          case Right(value) =>
            f.apply(value).map(_ => Response.empty)
        }
      case Notification(invalidMethod, _) =>
        fail(s"Expected method '$method', obtained '$invalidMethod'")
      case _ =>
        fail(s"Expected notification, obtained $message")
    }
  }

}
