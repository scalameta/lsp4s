package scala.meta.internal.jsonrpc

import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import monix.execution.Ack
import monix.reactive.Observer
import scribe.LoggerSupport

object MonixEnrichments {

  implicit class XtensionObserverCompanion[A](val `_`: Observer.type)
      extends AnyVal {
    def fromOutputStream(
        out: OutputStream,
        logger: LoggerSupport
    ): Observer.Sync[ByteBuffer] = {
      new Observer.Sync[ByteBuffer] {
        private[this] var isClosed: Boolean = false
        override def onNext(elem: ByteBuffer): Ack = {
          if (isClosed) Ack.Stop
          else {
            try {
              while (elem.hasRemaining) out.write(elem.get())
              out.flush()
              Ack.Continue
            } catch {
              case _: IOException =>
                logger.error("OutputStream closed!")
                isClosed = true
                Ack.Stop
            }
          }
        }
        override def onError(ex: Throwable): Unit = ()
        override def onComplete(): Unit = out.close()
      }
    }
  }

}
