// scalafmt: { maxColumn = 120 }
package scala.meta.jsonrpc

trait Logger {
  def trace(message: => String): Unit
  def trace(message: => String, cause: Throwable): Unit
  def debug(message: => String): Unit
  def debug(message: => String, cause: Throwable): Unit
  def info(message: => String): Unit
  def info(message: => String, cause: Throwable): Unit
  def warn(message: => String): Unit
  def warn(message: => String, cause: Throwable): Unit
  def error(message: => String): Unit
  def error(message: => String, cause: Throwable): Unit
}

object Logger {
  object StdoutLogger extends Logger {
    private def log(level: String, msg: => String, cause: Option[Throwable] = None): Unit = {
      println(s"[$level] $msg")
      cause.foreach(_.printStackTrace(Console.out))
    }
    override def trace(message: => String): Unit = log("trace", message)
    override def trace(message: => String, cause: Throwable): Unit = log("trace", message, Some(cause))
    override def debug(message: => String): Unit = log("debug", message)
    override def debug(message: => String, cause: Throwable): Unit = log("debug", message, Some(cause))
    override def info(message: => String): Unit = log("info", message)
    override def info(message: => String, cause: Throwable): Unit = log("info ", message, Some(cause))
    override def warn(message: => String): Unit = log("warn", message)
    override def warn(message: => String, cause: Throwable): Unit = log("warn ", message, Some(cause))
    override def error(message: => String): Unit = log("error", message)
    override def error(message: => String, cause: Throwable): Unit = log("error", message, Some(cause))
  }
}
