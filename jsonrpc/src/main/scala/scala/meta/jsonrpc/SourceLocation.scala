package scala.meta.jsonrpc

class SourceLocation(val file: String, val line: Int) {
  override def toString: String = s"$file:$line"
}

object SourceLocation {
  implicit def generate(
      implicit
      file: sourcecode.File,
      line: sourcecode.Line
  ): SourceLocation = {
    val slash = file.value.lastIndexOf(java.io.File.separatorChar)
    val idx = if (slash == -1) 0 else slash + 1
    new SourceLocation(file.value.substring(idx), line.value)
  }
}
