package scala.meta.jsonrpc

import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json

/** A JSON-RPC request ID, which can be a number, string or null */
sealed trait RequestId
object RequestId {
  def apply(n: Int): RequestId.String =
    RequestId.String(Json.fromString(n.toString))
  implicit val decoder: Decoder[RequestId] = Decoder.decodeJson.map {
    case s if s.isString => RequestId.String(s)
    case n if n.isNumber => RequestId.Number(n)
    case n if n.isNull => RequestId.Null
  }
  implicit val encoder: Encoder[RequestId] = Encoder.encodeJson.contramap {
    case RequestId.Number(v) => v
    case RequestId.String(v) => v
    case RequestId.Null => Json.Null
  }

  case class Number(value: Json) extends RequestId
  case class String(value: Json) extends RequestId
  case object Null extends RequestId
}
