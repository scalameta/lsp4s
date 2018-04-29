package scala.meta.jsonrpc

import perfolation._
import scribe._
import scribe.format._

object Logs {

  object ClassNameLine extends FormatBlock {
    override def format[M](record: LogRecord[M]): String = {
      val idx = record.className.lastIndexOf('.')
      val name =
        if (idx < 0) record.className
        else record.className.substring(idx + 1)
      val line =
        if (record.lineNumber.isDefined) record.lineNumber.get.toString
        else ""
      p"$name:$line"
    }
  }

  val Format = formatter"$level - $ClassNameLine $message$newLine"

  /** Update global settings for logging format. */
  def configure(): Unit = {
    Logger.root
      .clearHandlers()
      .withHandler(formatter = Format)
      .replace()
  }

}
