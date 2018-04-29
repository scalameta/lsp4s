package scala.meta.jsonrpc

import java.io.InputStream
import java.io.OutputStream

/** Wrapper around a pair of input/output streams. */
final class InputOutput(val in: InputStream, val out: OutputStream) {
  def withIn(in: InputStream): InputOutput = new InputOutput(in, out)
  def withOut(out: OutputStream): InputOutput = new InputOutput(in, out)
}
