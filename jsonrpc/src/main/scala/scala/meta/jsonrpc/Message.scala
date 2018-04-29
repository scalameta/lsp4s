package scala.meta.jsonrpc

import monix.eval.Task
import scala.meta.jsonrpc.pickle._
import ujson.Js

/** Supertype for all request, response and notification types. */
sealed trait Message
object Message {
  implicit val rw: ReadWriter[Message] = readwriter[Js].bimap[Message](
    { msg =>
      val obj = msg match {
        case r: Request => writeJs(r)
        case r: Notification => writeJs(r)
        case r: Response.Success => writeJs(r)
        case r: Response.Error => writeJs(r)
        case Response.Empty => Js.Obj()
      }
      obj.obj.put("jsonrpc", Js.Str("2.0"))
      obj
    }, { js =>
      if (js.obj.contains("id")) {
        if (js.obj.contains("error")) read[Response.Error](js)
        else if (js.obj.contains("result")) read[Response.Success](js)
        else read[Request](js)
      } else {
        read[Notification](js)
      }
    }
  )
}

@json final case class Request(
    method: String,
    params: Option[Js],
    id: RequestId
) extends Message {
  def toError(code: ErrorCode, message: String): Response =
    Response.error(ErrorObject(code, message, None), id)
}

@json final case class Notification(method: String, params: Option[Js])
    extends Message

/** Supertype for all response types. */
sealed trait Response extends Message {
  final def isSuccess: Boolean = this.isInstanceOf[Response.Success]
  final def isError: Boolean = this.isInstanceOf[Response.Error]
}

object Response {

  @json final case class Success(result: Js, id: RequestId) extends Response
  @json final case class Error(error: ErrorObject, id: RequestId)
      extends Response
  case object Empty extends Response

  def empty: Response = Empty
  def ok(result: Js, id: RequestId): Response =
    success(result, id)
  def okAsync[T](value: T): Task[Either[Response.Error, T]] =
    Task(Right(value))
  def success(result: Js, id: RequestId): Response =
    Success(result, id)
  def error(error: ErrorObject, id: RequestId): Response.Error =
    Error(error, id)
  def internalError(message: String): Response.Error =
    internalError(message, RequestId.Null)
  def internalError(message: String, id: RequestId): Response.Error =
    Error(ErrorObject(ErrorCode.InternalError, message, None), id)
  def invalidParams(message: String): Response.Error =
    invalidParams(message, RequestId.Null)
  def invalidParams(message: String, id: RequestId): Response.Error =
    Error(ErrorObject(ErrorCode.InvalidParams, message, None), id)
  def invalidRequest(message: String): Response.Error =
    Error(
      ErrorObject(ErrorCode.InvalidRequest, message, None),
      RequestId.Null
    )
  def cancelled(id: Js): Response.Error =
    Error(
      ErrorObject(ErrorCode.RequestCancelled, "", None),
      scala.meta.jsonrpc.pickle.read[RequestId](id)
    )
  def parseError(message: String): Response.Error =
    Error(ErrorObject(ErrorCode.ParseError, message, None), RequestId.Null)
  def methodNotFound(message: String, id: RequestId): Response.Error =
    Error(ErrorObject(ErrorCode.MethodNotFound, message, None), id)

}
