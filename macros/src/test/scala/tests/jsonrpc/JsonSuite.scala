package tests.jsonrpc

import minitest.SimpleTestSuite
import scala.meta.jsonrpc.json
import scala.meta.jsonrpc.pickle._

@json case class User(name: String, age: Option[Int] = None)

object JsonSuite extends SimpleTestSuite {
  test("read") {
    assertEquals(
      read[User]("""{"name": "John"}"""),
      User("John", None)
    )
    assertEquals(
      read[User]("""{"name": "Susan", "age": 42}"""),
      User("Susan", Some(42))
    )
  }

  test("write") {
    assertEquals(
      write(User("John", None)),
      """{"name":"John"}"""
    )
    assertEquals(
      write(User("Susan", Some(42))),
      """{"name":"Susan","age":42}"""
    )
  }

}
