package scala.meta.jsonrpc.testkit

import java.io.PipedInputStream
import java.io.PipedOutputStream
import monix.execution.Cancelable
import monix.execution.Scheduler
import scala.meta.jsonrpc.Connection
import scala.meta.jsonrpc.InputOutput
import scala.meta.jsonrpc.LanguageClient
import scala.meta.jsonrpc.Services

/**
 * A bi-directional connection between two running JSON-RPC entities named Alice and Bob.
 *
 * @param alice the running instance for Alice.
 * @param aliceIO the input/output streams for Alice.
 * @param bob the running instance for Bob.
 * @param bobIO the input/output streams for Bob.
 */
final class TestConnection(
    val alice: Connection,
    val aliceIO: InputOutput,
    val bob: Connection,
    val bobIO: InputOutput
) extends Cancelable {
  override def cancel(): Unit =
    Cancelable.cancelAll(alice :: bob :: bobIO :: aliceIO :: Nil)
}

object TestConnection {

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
      clientServices: LanguageClient => Services,
      serverServices: LanguageClient => Services
  )(implicit s: Scheduler): TestConnection = {
    val inAlice = new PipedInputStream()
    val inBob = new PipedInputStream()
    val outAlice = new PipedOutputStream(inBob)
    val outBob = new PipedOutputStream(inAlice)
    val aliceIO = new InputOutput(inAlice, outAlice)
    val bobIO = new InputOutput(inBob, outBob)
    val alice = Connection.simple(aliceIO, "alice")(clientServices)
    val bob = Connection.simple(bobIO, "bob")(serverServices)
    new TestConnection(alice, aliceIO, bob, bobIO)
  }

}
