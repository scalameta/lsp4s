package tests.jsonrpc

import minitest.SimpleTestSuite
import scala.meta.jsonrpc.Enums

sealed abstract class Weekday
object Weekday {
  case object Monday extends Weekday
  case object Tuesday extends Weekday
  case object Rest extends Weekday
  val values: IndexedSeq[Weekday] = Enums.findValues[Weekday]
}

object EnumsSuite extends SimpleTestSuite {
  test("findValues") {
    assertEquals(
      Weekday.values,
      IndexedSeq(
        Weekday.Monday,
        Weekday.Tuesday,
        Weekday.Rest
      )
    )
  }
}
