package scala.meta.jsonrpc.testkit

import java.io.PipedInputStream
import java.io.PipedOutputStream
import monix.execution.Scheduler
import scala.meta.jsonrpc.Connection
import scala.meta.jsonrpc.InputOutput
import scala.meta.jsonrpc.JsonRpcClient
import scala.meta.jsonrpc.Services

final case class ClientServer(client: Connection, server: Connection)

object ClientServer {

  /**
   * Instantiate bi-directional communication between a client and server.
   *
   * Useful for testing purposes.
   *
   * @param clientServices services implemented by the client.
   * @param serverServices services implemented by the server.
   * @param s the scheduler to run all services.
   */
  def apply(
      clientServices: JsonRpcClient => Services,
      serverServices: JsonRpcClient => Services
  )(implicit s: Scheduler): ClientServer = {
    val inClient = new PipedInputStream()
    val inServer = new PipedInputStream()
    val outClient = new PipedOutputStream(inServer)
    val outServer = new PipedOutputStream(inClient)
    val clientIO = new InputOutput(inClient, outClient)
    val serverIO = new InputOutput(inServer, outServer)
    val client = Connection(clientIO, "client")(clientServices)
    val server = Connection(serverIO, "server")(serverServices)
    new ClientServer(client, server)
  }
}
