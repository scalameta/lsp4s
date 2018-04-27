package scala.meta.internal.jsonrpc

import scala.meta.jsonrpc.json
import ujson.Js

@json case class CancelParams(id: Js)
