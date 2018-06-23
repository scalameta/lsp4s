package scala.meta.jsonrpc

import io.circe.Decoder
import io.circe.Encoder

sealed abstract class ErrorCode(val value: Int)
case object ErrorCode {
  case object ParseError extends ErrorCode(-32700)
  case object InvalidRequest extends ErrorCode(-32600)
  case object MethodNotFound extends ErrorCode(-32601)
  case object InvalidParams extends ErrorCode(-32602)
  case object InternalError extends ErrorCode(-32603)
  case object RequestCancelled extends ErrorCode(-32800)

  // Server error: -32000 to -32099
  case class Unknown(override val value: Int) extends ErrorCode(value)

  val builtin: Array[ErrorCode] = Array(
    ParseError,
    InvalidRequest,
    MethodNotFound,
    InvalidParams,
    InternalError,
    RequestCancelled
  )

  implicit val encoder: Encoder[ErrorCode] =
    Encoder.encodeInt.contramap(_.value)
  implicit val decoder: Decoder[ErrorCode] =
    Decoder.decodeInt.map(i => builtin.find(_.value == i).getOrElse(Unknown(i)))
}
