package scala.meta.jsonrpc

import ujson.Js

@json final case class ErrorObject(
    code: ErrorCode,
    message: String,
    data: Option[Js]
)
