package tests

import io.circe.Json
import minitest.SimpleTestSuite
import scala.meta.jsonrpc.ErrorCode
import scala.meta.jsonrpc.ErrorCode._
import io.circe.syntax._

object ErrorCodeSuite extends SimpleTestSuite {

  def check(code: Int, expected: ErrorCode): Unit =
    test(expected.toString) {
      val Right(obtained) = Json.fromInt(code).as[ErrorCode]
      assertEquals(obtained, expected)
      val obtainedJson = expected.asJson.noSpaces
      assertEquals(obtainedJson, code.toString)
    }

  check(666, Unknown(666))
  check(-32000, Unknown(-32000))
  check(-32099, Unknown(-32099))
  ErrorCode.builtin.foreach { code =>
    check(code.value, code)
  }

}
