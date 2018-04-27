package scala.meta.jsonrpc

import java.util.concurrent.ConcurrentLinkedQueue
import minitest.SimpleTestSuite
import monix.execution.Cancelable
import monix.execution.Scheduler.Implicits.global
import scala.collection.JavaConverters._
import scala.meta.jsonrpc.pickle._
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
 * | ---->>> |
 * | <<----- |
 *
 * Where:
 * ---> indicates notification
 * -->> indicates response
 * ->>> indicates request
 */
object PingPongSuite extends SimpleTestSuite {

  private val Ping = Endpoint.notification[String]("ping")
  private val Pong = Endpoint.notification[String]("pong")
  private val Hello = Endpoint.request[String, String]("hello")

  testAsync("ping pong") {
    val pongs = new ConcurrentLinkedQueue[String]()
    val services = Services
      .empty(Logger.root)
      .request(Hello) { msg =>
        s"$msg, World!"
      }
      .notification(Pong) { message =>
        assert(pongs.add(message))
      }
    def pongBack(client: JsonRpcClient): Services =
      services.notification(Ping) { message =>
        Pong.notify(message.replace("Ping", "Pong"))(client)
      }
    val ClientServer(client, server) =
      ClientServer(pongBack _, pongBack _)
    for {
      _ <- Ping.notify("Ping from client")(client.client)
      _ <- Ping.notify("Ping from server")(server.client)
      Right(obtained) <- Hello.request("Hello")(client.client).runAsync
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
