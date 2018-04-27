package scala.meta.jsonrpc

import monix.eval.Task
import scala.meta.internal.jsonrpc._
import scala.meta.jsonrpc.pickle.ReadWriter
import scribe.LoggerSupport
import ujson.Js

trait Service[A, B] {
  def handle(request: A): Task[B]
}

trait MethodName {
  def methodName: String
}

trait JsonRpcService extends Service[Message, Response]

trait NamedJsonRpcService extends JsonRpcService with MethodName

object Service {

  def request[A: ReadWriter, B: ReadWriter](method: String)(
      f: Service[A, Either[Response.Error, B]]
  ): NamedJsonRpcService = new NamedJsonRpcService {
    override def methodName: String = method
    override def handle(message: Message): Task[Response] = message match {
      case Request(`method`, params, id) =>
        params.getOrElse(Js.Null).asJsonDecoded[A] match {
          case Left(err) =>
            Task(Response.invalidParams(err.toString, id))
          case Right(value) =>
            f.handle(value).map {
              case Right(response) => Response.ok(response.asJsonEncoded, id)
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

  def notification[A: ReadWriter](method: String, logger: LoggerSupport)(
      f: Service[A, Unit]
  ): NamedJsonRpcService =
    new NamedJsonRpcService {
      override def methodName: String = method
      private def fail(msg: String): Task[Response] = Task {
        logger.error(msg)
        Response.empty
      }
      override def handle(message: Message): Task[Response] = message match {
        case Notification(`method`, params) =>
          params.getOrElse(Js.Null).asJsonDecoded[A] match {
            case Left(err) =>
              fail(s"Failed to parse notification $message. Errors: $err")
            case Right(value) =>
              f.handle(value).map(_ => Response.empty)
          }
        case Notification(invalidMethod, _) =>
          fail(s"Expected method '$method', obtained '$invalidMethod'")
        case _ =>
          fail(s"Expected notification, obtained $message")
      }
    }

}
