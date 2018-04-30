package scala.meta.jsonrpc

import perfolation._
import scribe._
import scribe.format._

object Logs {

  object FilenameLine extends FormatBlock {
    override def format[M](record: LogRecord[M]): String = {
      val idx = record.fileName.lastIndexOf('/')
      val name =
        if (idx < 0) record.fileName
        else record.fileName.substring(idx + 1)
      val line =
        if (record.lineNumber.isDefined) record.lineNumber.get.toString
        else ""
      p"$name:$line"
    }
  }

  val Format = formatter"$level - $FilenameLine $message$newLine"

  /** Update global settings for logging format. */
  def configure(): Unit = {
    Logger.root
      .clearHandlers()
      .withHandler(formatter = Format)
      .replace()
  }

}
