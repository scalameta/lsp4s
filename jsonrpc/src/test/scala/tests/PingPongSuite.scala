package tests

import java.util.concurrent.ConcurrentLinkedQueue
import minitest.SimpleTestSuite
import monix.execution.Scheduler.Implicits.global
import scala.collection.JavaConverters._
import scala.concurrent.Promise
import scala.meta.jsonrpc._
import scala.meta.jsonrpc.testkit._
import scribe.Logger

/**
 * Tests the following sequence:
 *
 * Alice     Bob
 * =============
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
    val conn = TestConnection(pongBack, pongBack)
    for {
      _ <- Ping.notify("Ping from client")(conn.alice.client)
      _ <- Ping.notify("Ping from server")(conn.bob.client)
      Right(helloWorld) <- Hello.request("Hello")(conn.alice.client).runAsync
      _ <- promise.future
    } yield {
      assertEquals(helloWorld, "Hello, World!")
      val obtainedPongs = pongs.asScala.toList.sorted
      val expectedPongs = List("Pong from client", "Pong from server")
      assertEquals(obtainedPongs, expectedPongs)
      conn.cancel()
    }
  }

}
