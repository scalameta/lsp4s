package tests

import java.util.concurrent.ConcurrentLinkedQueue
import minitest.SimpleTestSuite
import monix.eval.Task
import monix.execution.Cancelable
import scala.meta.jsonrpc._
import monix.execution.Scheduler.Implicits.global
import scala.collection.JavaConverters._
import scala.concurrent.Promise
import scala.meta.jsonrpc.Response.Success
import scala.meta.jsonrpc.testkit._
import scribe.Logger

/**
 * Tests the following sequence:
 *
 * C         S
 * |  Ping   |
 * | ------> |
 * |  Pong   |
 * | <------ |
 * |         |
 * |  Ping   |
 * | <------ |
 * |  Pong   |
 * | ------> |
 * |         |
 * |  Hello  |
 * | ----->> |
 * | <<<---- |
 *
 * Where:
 * ---> indicates notification
 * -->> indicates request
 * ->>> indicates response
 */
object PingPongSuite extends SimpleTestSuite {

  private val Ping = Endpoint.notification[String]("ping")
  private val Pong = Endpoint.notification[String]("pong")
  private val Hello = Endpoint.request[String, String]("hello")

  testAsync("ping pong") {
    val promise = Promise[Unit]()
    val pongs = new ConcurrentLinkedQueue[String]()
    val services = Services
      .empty(Logger.root)
      .request(Hello) { msg =>
        s"$msg, World!"
      }
      .notification(Pong) { message =>
        assert(pongs.add(message))
        if (pongs.size() == 2) {
          promise.complete(util.Success(()))
        }
      }
    val pongBack: LanguageClient => Services = { client =>
      services.notification(Ping) { message =>
        Pong.notify(message.replace("Ping", "Pong"))(client)
      }
    }
    val ClientServer(client, server) =
      ClientServer(pongBack, pongBack)
    for {
      _ <- Ping.notify("Ping from client")(client.client)
      _ <- Ping.notify("Ping from server")(server.client)
      Right(obtained) <- Hello.request("Hello")(client.client).runAsync
      _ <- promise.future
    } yield {
      assertEquals(obtained, "Hello, World!")
      val obtainedPongs = pongs.asScala.toList.sorted
      val expectedPongs =
        List("client", "server").map(name => s"Pong from $name")
      Cancelable.cancelAll(client :: server :: Nil)
      assertEquals(obtainedPongs, expectedPongs)
    }
  }

}
