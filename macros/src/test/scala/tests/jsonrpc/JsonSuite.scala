package tests.jsonrpc

import minitest.SimpleTestSuite
import scala.meta.jsonrpc.json
import scala.meta.jsonrpc.pickle._

sealed trait Sealed

@json case class User(
    name: String,
    age: Option[Int] = None,
    camelCase: String = ""
) extends Sealed
object User extends Foo with Bar {
  println(bar)
}
trait Foo
trait Bar {
  def bar = "bar"
}

object JsonSuite extends SimpleTestSuite {
  def checkRead(original: String, expected: User): Unit =
    test("read  " + original) {
      assertEquals(read[User](original), expected)
    }
  def checkWrite(original: User, expected: String): Unit =
    test("write " + original) {
      assertEquals(write[User](original), expected)
    }

  checkRead(
    """{"name": "John", "camelCase": "2"}""",
    User("John", None, "2")
  )
  checkRead(
    """{"name": "Susan", "age": 42}""",
    User("Susan", Some(42))
  )
  checkWrite(
    User("John", None),
    """{"name":"John"}"""
  )
  checkWrite(
    User("Susan", Some(42), "2"),
    """{"name":"Susan","age":42,"camelCase":"2"}"""
  )

}
