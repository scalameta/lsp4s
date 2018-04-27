package scala.meta.jsonrpc

import java.io.InputStream
import java.io.OutputStream

final class InputOutput(val in: InputStream, val out: OutputStream) {
  def withIn(in: InputStream): InputOutput = new InputOutput(in, out)
  def withOut(out: OutputStream): InputOutput = new InputOutput(in, out)
}
