package scala.meta.jsonrpc

import io.circe.Json
import io.circe.generic.JsonCodec

@JsonCodec final case class ErrorObject(
    code: ErrorCode,
    message: String,
    data: Option[Json]
)
