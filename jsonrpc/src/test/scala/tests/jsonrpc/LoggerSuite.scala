package tests.jsonrpc

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import minitest.SimpleTestSuite
import scala.meta.jsonrpc._

object LoggerSuite extends SimpleTestSuite {
  test("source location") {
    val source = SourceLocation.generate
    assert(source.toString.startsWith("LoggerSuite.scala:"))
  }

  test("stdout") {
    val baos = new ByteArrayOutputStream()
    Console.withOut(baos) {
      Logger.StdoutLogger.info("hello!", new Exception)
    }
    val obtained = baos.toString(StandardCharsets.UTF_8.name())
    assert(obtained.contains("LoggerSuite"), obtained)
    assert(obtained.contains("hello"), obtained)
    assert(obtained.contains("[info ]"), obtained)
  }
}
