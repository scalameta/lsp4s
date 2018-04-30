package scala.meta.jsonrpc

import cats.syntax.either._
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.generic.JsonCodec
import io.circe.syntax._
import monix.eval.Task

/** Supertype for all request, response and notification types. */
sealed trait Message
object Message {
  implicit val encoder: Encoder[Message] = new Encoder[Message] {
    override def apply(a: Message): Json = {
      val json = a match {
        case r: Request => r.asJson
        case r: Notification => r.asJson
        case r: Response.Success => r.asJson
        case r: Response.Error => r.asJson
        case Response.Empty => Json.obj()
      }
      json.mapObject(_.add("jsonrpc", "2.0".asJson))
    }
  }
  implicit val decoder: Decoder[Message] =
    Decoder.decodeJsonObject.emap { obj =>
      val json = Json.fromJsonObject(obj)
      val result =
        if (obj.contains("id"))
          if (obj.contains("error")) json.as[Response.Error]
          else if (obj.contains("result")) json.as[Response.Success]
          else json.as[Request]
        else json.as[Notification]
      result.leftMap(_.toString)
    }
}

@JsonCodec final case class Request(
    method: String,
    params: Option[Json],
    id: RequestId
) extends Message {
  def toError(code: ErrorCode, message: String): Response =
    Response.error(ErrorObject(code, message, None), id)
}

@JsonCodec final case class Notification(method: String, params: Option[Json])
    extends Message

/** Supertype for all response types. */
sealed trait Response extends Message {
  final def isSuccess: Boolean = this.isInstanceOf[Response.Success]
  final def isError: Boolean = this.isInstanceOf[Response.Error]
}

object Response {

  @JsonCodec final case class Success(result: Json, id: RequestId)
      extends Response
  @JsonCodec final case class Error(error: ErrorObject, id: RequestId)
      extends Response
  case object Empty extends Response

  def empty: Response = Empty
  def ok(result: Json, id: RequestId): Response =
    success(result, id)
  def okAsync[T](value: T): Task[Either[Response.Error, T]] =
    Task(Right(value))
  def success(result: Json, id: RequestId): Response =
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
  def cancelled(id: Json): Response.Error =
    Error(
      ErrorObject(ErrorCode.RequestCancelled, "", None),
      id.as[RequestId].getOrElse(RequestId.Null)
    )
  def parseError(message: String): Response.Error =
    Error(ErrorObject(ErrorCode.ParseError, message, None), RequestId.Null)
  def methodNotFound(message: String, id: RequestId): Response.Error =
    Error(ErrorObject(ErrorCode.MethodNotFound, message, None), id)

}
