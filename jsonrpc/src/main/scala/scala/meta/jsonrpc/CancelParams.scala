package scala.meta.jsonrpc

import io.circe.Json
import io.circe.generic.JsonCodec

@JsonCodec case class CancelParams(id: Json)
