package scala.meta.jsonrpc

import io.circe.Json
import io.circe.derivation.JsonCodec

@JsonCodec case class CancelParams(id: Json)
