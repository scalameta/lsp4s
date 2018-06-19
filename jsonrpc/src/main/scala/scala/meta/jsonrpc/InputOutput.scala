package scala.meta.jsonrpc

import java.io.InputStream
import java.io.OutputStream
import monix.execution.Cancelable

/** Wrapper around a pair of input/output streams. */
final class InputOutput(val in: InputStream, val out: OutputStream)
    extends Cancelable {
  override def cancel(): Unit = {
    Cancelable.cancelAll(
      List(
        Cancelable(() => in.close()),
        Cancelable(() => out.close())
      )
    )
  }
}
