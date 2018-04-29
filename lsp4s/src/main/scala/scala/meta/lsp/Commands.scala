package scala.meta.lsp

import scala.meta.jsonrpc.json
import ujson.Js

/**
 * Parameters and types used in the `initialize` message.
 */
@json case class InitializeParams(
    /**
     * The process Id of the parent process that started
     * the server.
     */
    processId: Double,
    /**
     * The rootPath of the workspace. Is null
     * if no folder is open.
     */
    rootPath: String,
    /**
     * The capabilities provided by the client (editor)
     */
    capabilities: ClientCapabilities
)

@json case class ClientCapabilities()

@json case class SaveOptions(
    /**
     * The client is supposed to include the content on save.
     */
    includeText: Option[Boolean] = None
)

@json case class TextDocumentSyncOptions(
    /**
     * Open and close notifications are sent to the server.
     */
    openClose: Option[Boolean] = None,
    /**
     * Change notifications are sent to the server. See TextDocumentSyncKind.None, TextDocumentSyncKind.Full
     * and TextDocumentSyncKind.Incremental.
     */
    change: Option[TextDocumentSyncKind] = None,
    /**
     * Will save notifications are sent to the server.
     */
    willSave: Option[Boolean] = None,
    /**
     * Will save wait until requests are sent to the server.
     */
    willSaveWaitUntil: Option[Boolean] = None,
    /**
     * Save notifications are sent to the server.
     */
    save: Option[SaveOptions] = None
)

@json case class ServerCapabilities(
    /**
     * Defines how text documents are synced.
     */
    textDocumentSync: Option[TextDocumentSyncOptions] = None,
    /**
     * The server provides hover support.
     */
    hoverProvider: Boolean = false,
    /**
     * The server provides completion support.
     */
    completionProvider: Option[CompletionOptions] = None,
    /**
     * The server provides signature help support.
     */
    signatureHelpProvider: Option[SignatureHelpOptions] = None,
    /**
     * The server provides goto definition support.
     */
    definitionProvider: Boolean = false,
    /**
     * The server provides find references support.
     */
    referencesProvider: Boolean = false,
    /**
     * The server provides document highlight support.
     */
    documentHighlightProvider: Boolean = false,
    /**
     * The server provides document symbol support.
     */
    documentSymbolProvider: Boolean = false,
    /**
     * The server provides workspace symbol support.
     */
    workspaceSymbolProvider: Boolean = false,
    /**
     * The server provides code actions.
     */
    codeActionProvider: Boolean = false,
    /**
     * The server provides code lens.
     */
    codeLensProvider: Option[CodeLensOptions] = None,
    /**
     * The server provides document formatting.
     */
    documentFormattingProvider: Boolean = false,
    /**
     * The server provides document range formatting.
     */
    documentRangeFormattingProvider: Boolean = false,
    /**
     * The server provides document formatting on typing.
     */
    documentOnTypeFormattingProvider: Option[DocumentOnTypeFormattingOptions] =
      None,
    /**
     * The server provides rename support.
     */
    renameProvider: Boolean = false,
    /**
     * The server provides execute command support.
     */
    executeCommandProvider: ExecuteCommandOptions = ExecuteCommandOptions(Nil)
)

@json case class CompletionOptions(
    resolveProvider: Boolean,
    triggerCharacters: Seq[String]
)
@json case class SignatureHelpOptions(triggerCharacters: Seq[String])
@json case class CodeLensOptions(resolveProvider: Boolean = false)
@json case class DocumentOnTypeFormattingOptions(
    firstTriggerCharacter: String,
    moreTriggerCharacters: Seq[String]
)
@json case class ExecuteCommandOptions(commands: Seq[String])
@json case class CompletionList(
    isIncomplete: Boolean,
    items: Seq[CompletionItem]
)
@json case class InitializeResult(capabilities: ServerCapabilities)
@json case class Shutdown()
@json case class ShutdownResult()

/**
 * The show message request is sent from a server to a client to ask the client to display a
 * particular message in the user interface. In addition to the show message notification the
 * request allows to pass actions and to wait for an answer from the client.
 *
 * @param `type` The message type. @see [[MessageType]]
 * @param message The actual message
 * @param actions The message action items to present.
 */
@json case class ShowMessageRequestParams(
    `type`: MessageType,
    message: String,
    actions: Seq[MessageActionItem]
)

/**
 * A short title like 'Retry', 'Open Log' etc.
 */
@json case class MessageActionItem(title: String)

@json case class TextDocumentPositionParams(
    textDocument: TextDocumentIdentifier,
    position: Position
)

@json case class ReferenceParams(
    textDocument: TextDocumentIdentifier,
    position: Position,
    context: ReferenceContext
)

@json case class RenameParams(
    textDocument: TextDocumentIdentifier,
    position: Position,
    newName: String
)

@json case class CodeActionParams(
    textDocument: TextDocumentIdentifier,
    range: Range,
    context: CodeActionContext
)

@json case class CodeActionRequest(params: CodeActionParams)

@json case class DocumentSymbolParams(textDocument: TextDocumentIdentifier)

@json case class TextDocumentRenameRequest(params: RenameParams)

@json case class ApplyWorkspaceEditResponse(applied: Boolean)
@json case class ApplyWorkspaceEditParams(
    label: Option[String] = None,
    edit: WorkspaceEdit
)

@json case class Hover(contents: Seq[MarkedString], range: Option[Range] = None)

///////////////////////////// Notifications ///////////////////////////////

// From server to client

@json case class ShowMessageParams(`type`: MessageType, message: String)
@json case class LogMessageParams(`type`: MessageType, message: String)
@json case class PublishDiagnostics(
    uri: String,
    diagnostics: Seq[Diagnostic]
)

@json case class DidOpenTextDocumentParams(textDocument: TextDocumentItem)
@json case class DidChangeTextDocumentParams(
    textDocument: VersionedTextDocumentIdentifier,
    contentChanges: Seq[TextDocumentContentChangeEvent]
)
@json case class DidCloseTextDocumentParams(
    textDocument: TextDocumentIdentifier
)
@json case class WillSaveTextDocumentParams(
    textDocument: TextDocumentIdentifier,
    reason: TextDocumentSaveReason
)
@json case class DidSaveTextDocumentParams(
    textDocument: TextDocumentIdentifier
)
@json case class DidChangeWatchedFilesParams(changes: Seq[FileEvent])
@json case class DidChangeConfigurationParams(settings: Js)

@json case class Initialized()

@json case class CancelRequest(id: Int)

@json case class CodeActionResult(params: Seq[Command])

@json case class SignatureHelp(
    signatures: Seq[SignatureInformation],
    activeSignature: Option[Int] = None,
    activeParameter: Option[Int] = None
)
@json case class WorkspaceSymbolResult(params: Seq[SymbolInformation])
