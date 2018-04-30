package scala.meta.internal.jsonrpc

import io.circe.generic.JsonCodec
import io.circe.Json

@JsonCodec case class CancelParams(id: Json)
