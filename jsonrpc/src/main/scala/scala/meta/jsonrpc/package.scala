package scala.meta

package object jsonrpc {

  @deprecated("Use Client instead", "0.1.0")
  type JsonRpcClient = Client
  @deprecated("Use Client instead", "0.1.0")
  val JsonRpcClient = Client

  type Js = io.circe.Json

}
