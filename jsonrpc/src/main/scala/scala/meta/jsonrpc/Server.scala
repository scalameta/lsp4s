package scala.meta.jsonrpc

import io.circe.Json
import io.circe.jawn.parseByteBuffer
import io.circe.syntax._
import java.nio.ByteBuffer
import monix.eval.Task
import monix.execution.Cancelable
import monix.execution.Scheduler
import monix.reactive.Observable
import scala.collection.concurrent.TrieMap
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.meta.internal.jsonrpc._
import scala.util.control.NonFatal
import scribe.LoggerSupport

/**
 * A JSON-RPC server to send+receive requests+responses+notifications with support to handle requests.
 *
 * The difference between this and `Client` is the ability to handle requests through the
 * `services` parameter.
 *
 * Request cancellation is implemented according to the Language Server Protocol through the
 * $/cancelRequest method. Observe that the JSON-RPC spec does have a notion of request cancellation.
 */
final class Server private (
    in: Observable[BaseProtocolMessage],
    client: Client,
    services: Services,
    requestScheduler: Scheduler,
    logger: LoggerSupport
) {

  private val activeClientRequests: TrieMap[Json, Cancelable] = TrieMap.empty
  private val cancelNotification = Service
    .notification[CancelParams]("$/cancelRequest", logger) { params =>
      val id = params.id
      activeClientRequests.get(id) match {
        case None =>
          Task {
            logger.warn(
              s"Can't cancel request $id, no active request found."
            )
            Response.empty
          }
        case Some(request) =>
          Task {
            logger.info(s"Cancelling request $id")
            request.cancel()
            activeClientRequests.remove(id)
            Response.cancelled(id)
          }
      }
    }
  private val handlersByMethodName: Map[String, Service] =
    services.addService(cancelNotification).byMethodName

  private def handleValidMessage(message: Message): Task[Response] =
    message match {
      case response: Response =>
        Task {
          client.clientRespond(response)
          Response.empty
        }
      case Notification(method, _) =>
        handlersByMethodName.get(method) match {
          case None =>
            Task {
              // Can't respond to invalid notifications
              logger.error(s"Unknown method '$method'")
              Response.empty
            }
          case Some(handler) =>
            handler
              .handle(message)
              .map {
                case Response.Empty => Response.empty
                case nonEmpty =>
                  logger.error(
                    s"Obtained non-empty response $nonEmpty for notification $message. " +
                      s"Expected Response.empty"
                  )
                  Response.empty
              }
              .onErrorRecover {
                case NonFatal(e) =>
                  logger.error(s"Error handling notification $message", e)
                  Response.empty
              }
        }
      case request @ Request(method, _, id) =>
        handlersByMethodName.get(method) match {
          case None =>
            Task {
              logger.info(s"Method not found '$method'")
              Response.methodNotFound(method, id)
            }
          case Some(handler) =>
            val response = handler.handle(request).onErrorRecover {
              case NonFatal(e) =>
                logger.error(s"Unhandled error handling request $request", e)
                Response.internalError(e.getMessage, request.id)
            }
            val runningResponse = response.runAsync(requestScheduler)
            activeClientRequests.put(request.id.asJson, runningResponse)
            Task.fromFuture(runningResponse)
        }

    }

  private def handleMessage(message: BaseProtocolMessage): Task[Response] =
    parseByteBuffer(ByteBuffer.wrap(message.content)) match {
      case Left(err) => Task.now(Response.parseError(err.toString))
      case Right(json) =>
        json.as[Message] match {
          case Left(err) => Task.now(Response.invalidRequest(err.toString))
          case Right(msg) => handleValidMessage(msg)
        }
    }

  /** Task to indefinitely listen to and respond to requests from the input stream. */
  def startTask: Task[Unit] =
    in.foreachL { msg =>
      handleMessage(msg)
        .map(client.serverRespond)
        .onErrorRecover {
          case NonFatal(e) =>
            logger.error("Unhandled error", e)
        }
        .runAsync(requestScheduler)
    }

  /** Listen indefinitely and respond to requests from the input stream. */
  def listen(): Unit = {
    val f = startTask.runAsync(requestScheduler)
    logger.info("Listening....")
    Await.result(f, Duration.Inf)
  }

}

object Server {

  def apply(
      in: Observable[BaseProtocolMessage],
      client: Client,
      services: Services,
      requestScheduler: Scheduler,
      logger: LoggerSupport
  ): Server =
    new Server(in, client, services, requestScheduler, logger)

}
