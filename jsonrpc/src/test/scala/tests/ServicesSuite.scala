package tests

import minitest.SimpleTestSuite
import scribe.Logger
import scala.meta.jsonrpc._

object ServicesSuite extends SimpleTestSuite {
  test("duplicate method throws IllegalArgumentException") {
    val duplicate = Endpoint.notification[Int]("duplicate")
    val base = Services.empty(Logger.root).notification(duplicate)(_ => ())
    intercept[IllegalArgumentException] {
      base.notification(duplicate)(_ => ())
    }
  }
}
