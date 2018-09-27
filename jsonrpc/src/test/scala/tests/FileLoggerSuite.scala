package tests

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import minitest.SimpleTestSuite

object FileLoggerSuite extends SimpleTestSuite {
  test("logs don't go to stdout") {
    val path = Files.createTempFile("lsp4s", ".log")
    val baos = new ByteArrayOutputStream()
    val writer = scribe.writer.FileWriter().path(_ => path).autoFlush
    Console.withOut(new PrintStream(baos)) {
      val logger = scribe.Logger("lsp4s").orphan().withHandler(writer = writer)
      logger.info("This is info")
      logger.warn("This is warning")
      logger.error("This is error")
    }
    val obtainedOut = baos.toString()
    assert(obtainedOut.isEmpty)
    val obtainedLogs =
      new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
    List("info", "warning", "error").foreach { message =>
      assert(obtainedLogs.contains(s"This is $message"), obtainedLogs)
    }
  }
}
