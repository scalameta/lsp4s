// scalafmt: { maxColumn = 120 }
package scala.meta.lsp

import io.circe.Json
import scala.meta.jsonrpc._
import scala.meta.jsonrpc.Endpoint._
import scala.meta.jsonrpc.Response.Error
import monix.eval.Task

object Lifecycle extends Lifecycle
trait Lifecycle {
  val initialized: Endpoint[Json, Unit] =
    notification("initialized")
  val initialize: Endpoint[InitializeParams, InitializeResult] =
    request[InitializeParams, InitializeResult]("initialize")
  val shutdown: Endpoint[Json, Json] =
    request("shutdown")
  val exit: Endpoint[Json, Unit] =
    notification("exit")
}

object TextDocument extends TextDocument
trait TextDocument {
  val publishDiagnostics: Endpoint[PublishDiagnostics, Unit] =
    notification[PublishDiagnostics]("textDocument/publishDiagnostics")
  val didClose: Endpoint[DidCloseTextDocumentParams, Unit] =
    notification[DidCloseTextDocumentParams]("textDocument/didClose")
  val willSave: Endpoint[WillSaveTextDocumentParams, Unit] =
    notification[WillSaveTextDocumentParams]("textDocument/willSave")
  val willSaveWaitUntil: Endpoint[WillSaveTextDocumentParams, List[TextEdit]] =
    request[WillSaveTextDocumentParams, List[TextEdit]]("textDocument/willSaveWaitUntil")
  val didSave: Endpoint[DidSaveTextDocumentParams, Unit] =
    notification[DidSaveTextDocumentParams]("textDocument/didSave")
  val didOpen: Endpoint[DidOpenTextDocumentParams, Unit] =
    notification[DidOpenTextDocumentParams]("textDocument/didOpen")
  val didChange: Endpoint[DidChangeTextDocumentParams, Unit] =
    notification[DidChangeTextDocumentParams]("textDocument/didChange")
  val completion: Endpoint[TextDocumentPositionParams, CompletionList] =
    request[TextDocumentPositionParams, CompletionList]("textDocument/completion")
  val definition: Endpoint[TextDocumentPositionParams, List[Location]] =
    request[TextDocumentPositionParams, List[Location]]("textDocument/definition")
  val references: Endpoint[ReferenceParams, List[Location]] =
    request[ReferenceParams, List[Location]]("textDocument/references")
  val hover: Endpoint[TextDocumentPositionParams, Hover] =
    request[TextDocumentPositionParams, Hover]("textDocument/hover")
  val codeAction: Endpoint[CodeActionParams, List[Command]] =
    request[CodeActionParams, List[Command]]("textDocument/codeAction")
  val documentHighlight: Endpoint[TextDocumentPositionParams, List[DocumentHighlight]] =
    request[TextDocumentPositionParams, List[DocumentHighlight]]("textDocument/documentHighlight")
  val documentSymbol: Endpoint[DocumentSymbolParams, List[SymbolInformation]] =
    request[DocumentSymbolParams, List[SymbolInformation]]("textDocument/documentSymbol")
  val formatting: Endpoint[DocumentFormattingParams, List[TextEdit]] =
    request[DocumentFormattingParams, List[TextEdit]]("textDocument/formatting")
  val rename: Endpoint[RenameParams, WorkspaceEdit] =
    request[RenameParams, WorkspaceEdit]("textDocument/rename")
  val signatureHelp: Endpoint[TextDocumentPositionParams, SignatureHelp] =
    request[TextDocumentPositionParams, SignatureHelp]("textDocument/signatureHelp")
}

object Workspace extends Workspace
trait Workspace {
  val didChangeConfiguration: Endpoint[DidChangeConfigurationParams, Unit] =
    notification[DidChangeConfigurationParams]("workspace/didChangeConfiguration")
  val didChangeWatchedFiles: Endpoint[DidChangeWatchedFilesParams, Unit] =
    notification[DidChangeWatchedFilesParams]("workspace/didChangeWatchedFiles")
  val executeCommand: Endpoint[ExecuteCommandParams, Json] =
    request[ExecuteCommandParams, Json]("workspace/executeCommand")
  val symbol: Endpoint[WorkspaceSymbolParams, List[SymbolInformation]] =
    request[WorkspaceSymbolParams, List[SymbolInformation]]("workspace/symbol")
  val applyEdit: Endpoint[ApplyWorkspaceEditParams, ApplyWorkspaceEditResponse] =
    request[ApplyWorkspaceEditParams, ApplyWorkspaceEditResponse]("workspace/applyEdit")
}

object Window extends Window
trait Window {
  object showMessage extends Endpoint[ShowMessageParams, Unit]("window/showMessage") {
    def error(message: String)(implicit client: Client): Unit =
      showMessage.notify(ShowMessageParams(MessageType.Error, message))
    def warn(message: String)(implicit client: Client): Unit =
      showMessage.notify(ShowMessageParams(MessageType.Warning, message))
    def info(message: String)(implicit client: Client): Unit =
      showMessage.notify(ShowMessageParams(MessageType.Info, message))
    def log(message: String)(implicit client: Client): Unit =
      showMessage.notify(ShowMessageParams(MessageType.Log, message))
  }
  object showMessageRequest
      extends Endpoint[ShowMessageRequestParams, Option[MessageActionItem]]("window/showMessageRequest") {
    def error(message: String)(actionItems: MessageActionItem*)(
        implicit client: Client): Task[Either[Error, Option[MessageActionItem]]] =
      showMessageRequest.request(
        ShowMessageRequestParams(MessageType.Error, message, actionItems)
      )
    def warn(message: String)(actionItems: MessageActionItem*)(
        implicit client: Client): Task[Either[Error, Option[MessageActionItem]]] =
      showMessageRequest.request(
        ShowMessageRequestParams(MessageType.Warning, message, actionItems)
      )
    def info(message: String)(actionItems: MessageActionItem*)(
        implicit client: Client): Task[Either[Error, Option[MessageActionItem]]] =
      showMessageRequest.request(
        ShowMessageRequestParams(MessageType.Info, message, actionItems)
      )
    def log(message: String)(actionItems: MessageActionItem*)(
        implicit client: Client): Task[Either[Error, Option[MessageActionItem]]] =
      showMessageRequest.request(
        ShowMessageRequestParams(MessageType.Log, message, actionItems)
      )
  }
  object logMessage extends Endpoint[LogMessageParams, Unit]("window/logMessage") {
    def error(message: String)(implicit client: Client): Unit =
      super.notify(LogMessageParams(MessageType.Error, message))
    def warn(message: String)(implicit client: Client): Unit =
      super.notify(LogMessageParams(MessageType.Warning, message))
    def info(message: String)(implicit client: Client): Unit =
      super.notify(LogMessageParams(MessageType.Info, message))
    def log(message: String)(implicit client: Client): Unit =
      super.notify(LogMessageParams(MessageType.Log, message))
  }
}
