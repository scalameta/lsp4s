[![Build Status](https://travis-ci.org/scalameta/lsp4s.svg?branch=master)](https://travis-ci.org/scalameta/lsp4s)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.scalameta/lsp4s_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.scalameta/lsp4s_2.12)

# lsp4s

This is a Scala implementation for [JSON-RPC][] and the [Language Server
Protocol][lsp] with a emphasis on compile-time safety and extensibility. This
project contains two modules:

- `jsonrpc`: core data structures and client/server implementations for JSON-RPC
  with additional support for cancellation via method `$/cancelRequest`.
- `lsp4s`: data structures for the Language Server Protocol.

The `jsonrpc` module has the following dependencies:

- [Monix][]: for asynchronous programming primitives `Task` and `Observable`.
  Monix `Task` was chosen over the standard library `Future` in order to support
  cancellation, which is an important part of the Language Server Protocol.
  Monix `Observable` is also a good fit the stream-based nature of the JSON-RPC
  protocol.
- [Circe][]: for JSON serialization and de-serialization. Our requirements for
  JSON include a parser, printer, custom decoding/encoding APIs as well as
  automatic (or semi-automatic) derivation for case classes. Circe was chosen
  over alternative JSON libraries for its high quality error handling and
  type-safe APIs with strong compile-time guarantees.
- [Scribe][]: for logging. Scribe was chosen over alternative logging frameworks
  (such as slf4j) for its usage of compile-time reflection over runtime
  reflection in addition to a fully programmable configuration API avoiding the
  need for XML.

The `lsp4s` module has the following dependencies:

- `jsonrpc`: the module in this project
- [Enumeratum]: for reflection-free and boilerplate-free enumerations.

[enumeratum]: https://github.com/lloydmeta/enumeratum
[json-rpc]: http://www.jsonrpc.org
[lsp]: https://microsoft.github.io/language-server-protocol/
[monix]: https://monix.io/
[circe]: https://circe.github.io/circe/
[scribe]: https://github.com/outr/scribe

## Getting Started

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.scalameta/lsp4s_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.scalameta/lsp4s_2.12)

To implement a language server with lsp4s, add the following dependency:

```scala
libraryDependencies += "org.scalameta" %% "lsp4s" % "VERSION"
```

In the following example, we are going to implement a handler for the LSP
[`textDocument/hover`][] request. In addition to the request handler, we need to
handle the lifecycle requests and notifications `initialize`, `initialized`,
`shutdown` and `exit`.

```scala
import io.circe.Json        // to construct Json values
import scala.meta.jsonrpc._ // for JSON-RPC APIs
import scala.meta.lsp._     // for LSP endpoints
import scribe._             // for logging

def myServices(logger: LoggerSupport, client: LanguageClient): Services = {
  Services
    .empty(logger)
    .request(Lifecycle.initialize) { params =>
      logger.info(params.toString)
      val capabilities = ServerCapabilities(
        hoverProvider = true
      )
      InitializeResult(capabilities)
    }
    .notification(Lifecycle.initialized) { _ =>

      // NOTE: It's possible to send a notification to the client at any point
      Window.showMessage.info("Hello world!")(client)

      // notification handlers can't respond so return Unit
      logger.info("Client is initialized")
    }
    .request(Lifecycle.shutdown) { _ =>
      logger.info("Client is about to call exit soon")
      Json.Null
    }
    .notification(Lifecycle.exit) { _ =>
      logger.info("Goodbye!")
      System.exit(0)
    }
    .request(TextDocument.hover) { params =>

      // Publish a dummy error message, normally diagnostics are published  after textDocument/didChange
      // or textDocument/didSave with error messages from the compiler or build tool.
      TextDocument.publishDiagnostics.notify(
        PublishDiagnostics(
          uri = params.textDocument.uri,
          diagnostics = List(
            Diagnostic(
              range = Range(start = params.position, end = params.position),
              severity = Some(DiagnosticSeverity.Error),
              code = Some("hover-error"),
              source = Some("my-project"),
              message = "This is an example red error message!"
            )
          )
        )
      )(client)

      // Return a dummy response for this example, normally the implementation will call the compiler
      // for type information here and display a type signature and docstrings.
      Hover(List(MarkdownString("**Hello** from server!")), range = None)
    }
}
```

Now that we have implemented our services, let's wire everything together

```scala
val scheduler =
  monix.execution.Scheduler(java.util.concurrent.Executors.newFixedThreadPool(4))

// Construct an InputStream and OutputStream pair. For LSP, this is normally System.in and
// System.out but to connect with for example sbt server you use sockets: https://github.com/sbt/ipcsocket
val io = new InputOutput(System.in, System.out)


// Construct logger that appends to file, don't log to stdout since that is reserved for
// JSON-RPC communication.
val logPath = java.nio.file.Paths.get("my-logger.log").toAbsolutePath
val fileWriter = scribe.writer.FileWriter().path(timestamp => logPath).autoFlush
val logger = Logger("my-logger").orphan().withHandler(writer = fileWriter)

// Establish a connection with the client, this fires up the server and starts listening for
// requests and notifications until the input stream closes.
val connection =
  Connection(io, serverLogger = logger, clientLogger = logger) { client =>
    myServices(logger, client)
  }(scheduler)

// Wait forever until the client closes the connection.
scala.concurrent.Await.result(
  connection.server,
  scala.concurrent.duration.Duration.Inf
)
```

For a full executable example, see
[PingPongSuite](lsp4s/jsonrpc/src/test/scala/tests/PingPongSuite.scala).

It's possible to use only the JSON-RPC parts of this library without the LSP
endpoints. Instead of depending on the `lsp4s` module, use the following module
instead:

```scala
libraryDependencies += "org.scalameta" %% "jsonrpc" % "VERSION"
```

[`textdocument/hover`]:
  https://microsoft.github.io/language-server-protocol/specification#textDocument_hover

### Used by

The modules `lsp4s` and `jsonrpc` are used in the following projects:

- [Metals][]: a language server for Scala that uses `lsp4s` to communicate with
  editors and `jsonrpc` to communicate with sbt server as a client.
- [IntelliJ Scala][]: the most widely used IDE for Scala uses `jsonrpc` to
  communicate with build tools through the Build Server Protocol.
- [Bloop][]: Scala build server and command-line tool for fast compile and test
  workflows that uses `jsonrpc` to communicate with IDEs through the Build
  Server Protocol.
- [Build Server Protocol][bsp]: a complementary protocol to the Language Server
  Protocol targeted for build tools that uses `jsonrpc` to provide a `bsp` Scala
  library similar to the `lsp4s` module in this project.

Are you using `lsp4s` or `jsonrpc`? Submit a pull request adding your project to
the list :)

[bsp]: https://github.com/scalacenter/bsp/blob/master/docs/bsp.md
[bloop]: https://scalacenter.github.io/bloop/
[intellij scala]: https://github.com/JetBrains/intellij-scala
[metals]: https://scalameta.org/metals/

### Team

The current maintainers (people who can merge pull requests) are:

- Alexey Alekhin - [`@laughedelic`](https://github.com/laughedelic)
- Gabriele Petronella - [`@gabro`](https://github.com/gabro)
- Jorge Vicente Cantero - [`@jvican`](https://github.com/jvican)
- Ólafur Páll Geirsson - [`@olafurpg`](https://github.com/olafurpg)

An up-to-date list of contributors is available here:
https://github.com/scalameta/lsp4s/graphs/contributors

## Alternatives

We recommend looking into [lsp4j][] if you want to implement a language server
or do JSON-RPC on the JVM. It's a pure Java implementation with a smaller
dependency footprint than lsp4s. It's used by the official Java Language Server
and is actively maintained. Before starting lsp4s, we made several attempts to
use lsp4j but encountered cryptic runtime-reflection errors that we struggled to
resolve. The emphasis in lsp4s on compile-time safety is partly motivated by
this experience.

For a Scala alternative, there is [dragos-vscode-scala][] that builds on top of
[scala-json-rpc][] and uses the standard library `Future`. We used
dragos-vscode-scala for several months and it was helpful in getting us
off-the-ground early on. In fact, the lsp4s JSON-RPC header parser is a fork of
the header parser in dragos-scode-scala (see [NOTICE](NOTICE.md)). As our usage
of dragos-vscode-scala grew we found it difficult to add new LSP endpoints such
as `workspace/applyEdit` or build new protocols such as [BSP][]. We encountered
several times cryptic runtime errors during manual integration testing, which
hurt our productivity. The lsp4s `Endpoint` abstraction builds on top of this
experience by reducing ceremony for adding new endpoints while lowering the risk
of serialization errors at runtime.

[scala-json-rpc]: https://github.com/dhpiggott/scala-json-rpc
[dragos-vscode-scala]: https://github.com/dragos/dragos-vscode-scala
[lsp4j]: https://github.com/eclipse/lsp4j
[bsp]: https://github.com/scalacenter/bsp/blob/master/docs/bsp.md
