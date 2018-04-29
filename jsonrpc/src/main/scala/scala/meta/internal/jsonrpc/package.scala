package scala.meta.internal

import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import monix.execution.Ack
import monix.reactive.Observer
import scala.meta.jsonrpc.pickle._
import scala.util.control.NonFatal
import scribe.LoggerSupport
import ujson.BytesRenderer
import ujson.Js
import ujson.Transformable

package object jsonrpc {

  implicit class XtensionAsJson[T](e: T) {

    /** Parse Transformable into ujson.Js */
    def asJsonParsed(implicit conv: T => Transformable): Either[Throwable, Js] =
      try Right(ujson.read(e))
      catch { case NonFatal(ex) => Left(ex) }

    /** Converts T into ujson.Js with ReadWriter[T] writer */
    def asJsonEncoded(implicit writer: ReadWriter[T]): Js = writeJs(e)

    /** Converts T into a json encoded string with ReadWriter[T] writer */
    def asJsonString(implicit writer: ReadWriter[T]): String = write(e)

    /** Converts T into ujson.Js with ReadWriter[T] writer */
    def asBytesEncoded(implicit writer: ReadWriter[T]): Array[Byte] =
      transform(e).to(BytesRenderer()).toBytes

  }

  implicit class XtensionAsSyntax(js: Js) {

    /** Converts ujson.Js to T with ReadWriter[T] reader */
    def asJsonDecoded[T](implicit reader: ReadWriter[T]): Either[Throwable, T] =
      try Right(read[T](js))
      catch { case NonFatal(e) => Left(e) }

  }

  implicit class XtensionEitherLeftMap[A, B](either: Either[A, B]) {

    def leftMap[C](f: A => C): Either[C, B] =
      either match {
        case Left(e) => Left(f(e))
        case r => r.asInstanceOf[Either[C, B]]
      }

  }

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
