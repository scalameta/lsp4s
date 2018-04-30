package scala.meta.jsonrpc

import java.io.OutputStream
import java.nio.ByteBuffer
import monix.eval.Callback
import monix.eval.Task
import monix.execution.Ack
import monix.execution.Cancelable
import monix.execution.atomic.Atomic
import monix.execution.atomic.AtomicInt
import monix.reactive.Observer
import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.meta.internal.jsonrpc._
import scribe.LoggerSupport
import io.circe.syntax._
import cats.syntax.either._
import io.circe.Decoder
import io.circe.Encoder
import monix.execution.UncaughtExceptionReporter

/**
 * A JSON-RPC client to send+receive requests+responses+notifications with support to initiate requests.
 *
 * The difference between this and `Server` is the ability to initiate requests to be handled
 * by the other entity.
 *
 * Request cancellation is implemented according to the Language Server Protocol through the
 * $/cancelRequest method. Observe that the JSON-RPC spec does have a notion of request cancellation.
 */
final class Client private (
    out: Observer[ByteBuffer],
    logger: LoggerSupport
) {

  private val writer = new MessageWriter(out, logger)
  private val counter: AtomicInt = Atomic(1)
  private val activeServerRequests =
    TrieMap.empty[RequestId, Callback[Response]]

  /** Send notification to client using an Endpoint */
  def notify[A](
      endpoint: Endpoint[A, Unit],
      notification: A
  ): Future[Ack] =
    notify[A](endpoint.method, notification)(
      endpoint.encoderParams,
      endpoint.decoderParams
    )

  /**
   * Send notification to client with custom RPC method.
   *
   * @return The acknowledgement when the notification has been written
   *         to the wire. Observe that it's not possible for the client
   *         to respond to notifications.
   */
  def notify[A: Encoder: Decoder](
      method: String,
      notification: A
  ): Future[Ack] =
    writer.write(Notification(method, Some(notification.asJson)))

  /** Send request to client using an Endpoint */
  def request[A, B](
      endpoint: Endpoint[A, B],
      req: A
  ): Task[Either[Response.Error, B]] =
    request[A, B](endpoint.method, req)(
      endpoint.encoderParams,
      endpoint.decoderParams,
      endpoint.encoderResult,
      endpoint.decoderResult
    )

  /**
   * Send request to client with custom RPC method.
   *
   * This overload allows ad-hoc one-off endpoints, it's recommended to use the Endpoint
   * overload for improved readability and maintainability.
   *
   * @return The response from the client, which is either a successful
   *         value or a JSON-RPC error.
   */
  def request[A: Encoder: Decoder, B: Encoder: Decoder](
      method: String,
      request: A
  ): Task[Either[Response.Error, B]] = Task.defer {
    val nextId = RequestId(counter.incrementAndGet())
    val response = Task.create[Response] { (out, cb) =>
      val scheduled = out.scheduleOnce(Duration(0, "s")) {
        val json = Request(method, Some(request.asJson), nextId)
        activeServerRequests.put(nextId, cb)
        writer.write(json)
      }
      Cancelable { () =>
        scheduled.cancel()
        this.notify("$/cancelRequest", CancelParams(nextId.value))
      }
    }
    response.map {
      case Response.Empty =>
        Left(
          Response.invalidParams(
            s"Got empty response for request $request",
            nextId
          )
        )
      case err: Response.Error =>
        Left(err)
      case Response.Success(result, _) =>
        result.as[B].leftMap { err =>
          Response.invalidParams(err.toString, nextId)
        }
    }
  }

  /** Respond from server after client request. */
  private[jsonrpc] def serverRespond(response: Response): Future[Ack] =
    response match {
      case Response.Empty =>
        Ack.Continue
      case x: Response.Success =>
        writer.write(x)
      case x: Response.Error =>
        logger.error(s"Response error: $x")
        writer.write(x)
    }

  /** Respond from client after server request. */
  private[jsonrpc] def clientRespond(response: Response): Unit =
    for {
      id <- response match {
        case Response.Success(_, requestId) => Some(requestId)
        case Response.Error(_, requestId) => Some(requestId)
        case Response.Empty => None
      }
      callback <- activeServerRequests.get(id).orElse {
        logger.error(s"Response to unknown request: $response")
        None
      }
    } {
      activeServerRequests.remove(id)
      callback.onSuccess(response)
    }

}

object Client {
  def empty(logger: LoggerSupport): Client =
    Client(Observer.empty(new UncaughtExceptionReporter {
      override def reportFailure(ex: Throwable): Unit = logger.error(ex)
    }), scribe.Logger.root)
  def apply(
      out: Observer[ByteBuffer],
      logger: LoggerSupport
  ): Client =
    new Client(out, logger)

  def fromOutputStream(out: OutputStream, logger: LoggerSupport) =
    new Client(Observer.fromOutputStream(out, logger), logger)
}
