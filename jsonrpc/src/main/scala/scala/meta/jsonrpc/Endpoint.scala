package scala.meta.jsonrpc

import monix.eval.Task
import monix.execution.Ack
import scala.concurrent.Future
import scala.meta.jsonrpc.pickle.ReadWriter

class Endpoint[A: ReadWriter, B: ReadWriter](val method: String) {
  def readwriterA: ReadWriter[A] = implicitly[ReadWriter[A]]
  def readwriterB: ReadWriter[B] = implicitly[ReadWriter[B]]

  def request(request: A)(
      implicit client: JsonRpcClient
  ): Task[Either[Response.Error, B]] =
    client.request[A, B](method, request)
  def notify(
      notification: A
  )(implicit client: JsonRpcClient): Future[Ack] =
    client.notify[A](method, notification)
}

object Endpoint {

  def request[A: ReadWriter, B: ReadWriter](
      method: String
  ): Endpoint[A, B] =
    new Endpoint(method)

  def notification[A: ReadWriter](method: String): Endpoint[A, Unit] =
    new Endpoint(method)

}
