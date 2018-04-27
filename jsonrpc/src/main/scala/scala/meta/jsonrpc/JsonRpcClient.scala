package scala.meta.jsonrpc

import scala.concurrent.Future
import scala.meta.jsonrpc.pickle.ReadWriter
import monix.eval.Task
import monix.execution.Ack

trait JsonRpcClient {

  def notify[A: ReadWriter](
      method: String,
      notification: A
  ): Future[Ack]
  final def notify[A](
      endpoint: Endpoint[A, Unit],
      notification: A
  ): Future[Ack] =
    notify[A](endpoint.method, notification)(endpoint.readwriterA)

  def request[A: ReadWriter, B: ReadWriter](
      method: String,
      request: A
  ): Task[Either[Response.Error, B]]
  final def request[A, B](
      endpoint: Endpoint[A, B],
      req: A
  ): Task[Either[Response.Error, B]] =
    request[A, B](endpoint.method, req)(
      endpoint.readwriterA,
      endpoint.readwriterB
    )

  // Advanced
  def serverRespond(response: Response): Future[Ack]
  def clientRespond(response: Response): Unit

}

object JsonRpcClient {
  val empty: JsonRpcClient = new JsonRpcClient {
    override def notify[A: ReadWriter](
        method: String,
        notification: A
    ): Future[Ack] = Ack.Continue
    override def request[A: ReadWriter, B: ReadWriter](
        method: String,
        request: A
    ): Task[Either[Response.Error, B]] = Task.never

    override def serverRespond(response: Response): Future[Ack] = Ack.Continue
    override def clientRespond(response: Response): Unit = ()
  }
}
