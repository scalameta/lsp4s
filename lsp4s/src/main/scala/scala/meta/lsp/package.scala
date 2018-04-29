package scala.meta

import scala.meta.jsonrpc.Client
import scala.meta.jsonrpc.Server

package object lsp {

  @deprecated("Use Server instead", "0.1.0")
  type LanguageServer = Server
  @deprecated("Use Server instead", "0.1.0")
  val LanguageServer = Server

  @deprecated("Use Client instead", "0.1.0")
  type LanguageClient = Client
  @deprecated("Use Client instead", "0.1.0")
  val LanguageClient = Client

}
