package scala.meta.internal

import scala.meta.jsonrpc.pickle._
import scala.util.control.NonFatal
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

}
