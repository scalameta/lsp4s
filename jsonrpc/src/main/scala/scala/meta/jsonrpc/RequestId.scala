package scala.meta.jsonrpc

import ujson.Js
import scala.meta.jsonrpc.pickle._

/** A JSON-RPC request ID, which can be a number, string or null */
sealed trait RequestId
object RequestId {
  def apply(n: Int): RequestId.String =
    RequestId.String(Js.Str(n.toString))
  implicit val decoder: ReadWriter[RequestId] =
    readwriter[Js].bimap[RequestId](
      {
        case Number(js) => js
        case String(js) => js
        case Null => Js.Null
      }, {
        case str: Js.Str => String(str)
        case num: Js.Num => Number(num)
        case _ => Null
      }
    )
  case class Number(value: Js) extends RequestId
  case class String(value: Js) extends RequestId
  case object Null extends RequestId
}
