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
import scala.meta.jsonrpc.pickle.ReadWriter
import scala.meta.internal.jsonrpc._
import MonixEnrichments._
import scribe.LoggerSupport

final class LanguageClient private (
    out: Observer[ByteBuffer],
    logger: LoggerSupport
) extends JsonRpcClient {

  private val writer = new MessageWriter(out, logger)
  private val counter: AtomicInt = Atomic(1)
  private val activeServerRequests =
    TrieMap.empty[RequestId, Callback[Response]]

  override def notify[A: ReadWriter](
      method: String,
      notification: A
  ): Future[Ack] =
    writer.write(Notification(method, Some(notification.asJsonEncoded)))

  override def request[A: ReadWriter, B: ReadWriter](
      method: String,
      request: A
  ): Task[Either[Response.Error, B]] = {
    val nextId = RequestId(counter.incrementAndGet())
    val response = Task.create[Response] { (out, cb) =>
      val scheduled = out.scheduleOnce(Duration(0, "s")) {
        val json = Request(method, Some(request.asJsonEncoded), nextId)
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
        result.asJsonDecoded[B].leftMap { err =>
          Response.invalidParams(err.toString, nextId)
        }
    }
  }

  override def serverRespond(response: Response): Future[Ack] = response match {
    case Response.Empty => Ack.Continue
    case x: Response.Success => writer.write(x)
    case x: Response.Error =>
      logger.error(s"Response error: $x")
      writer.write(x)
  }
  override def clientRespond(response: Response): Unit =
    for {
      id <- response match {
        case Response.Empty => None
        case Response.Success(_, requestId) => Some(requestId)
        case Response.Error(_, requestId) => Some(requestId)
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

object LanguageClient {
  def apply(out: Observer[ByteBuffer], logger: LoggerSupport): LanguageClient =
    new LanguageClient(out, logger)

  def apply(out: OutputStream, logger: LoggerSupport) =
    new LanguageClient(Observer.fromOutputStream(out, logger), logger)
}
