package scala.meta.jsonrpc

import scala.language.experimental.macros
import scala.language.higherKinds
import upickle.internal.Macros.Reading
import upickle.internal.Macros.Writing

/**
 * Custom upickle pickler that emits `null` values for None.
 *
 * To learn more: http://www.lihaoyi.com/upickle/#CustomConfiguration
 */
object pickle extends upickle.AttributeTagged {

  override implicit def OptionWriter[T: Writer]: Writer[Option[T]] =
    implicitly[Writer[T]].comap[Option[T]] {
      case None => null.asInstanceOf[T]
      case Some(x) => x
    }

  override implicit def OptionReader[T: Reader]: Reader[Option[T]] =
    implicitly[Reader[T]].mapNulls {
      case null => None
      case x => Some(x)
    }

  // classes that extend a `sealed` base class write and read a
  // `"$type": "fully.qualified.Name"`
  // value in the produced dictionary. The customizations below
  // override the behavior to that sealed traits don't include $type annotations.

  override def macroR0[T, M[_]]: pickle.Reader[T] = macro macroRImpl[T, M]
  override def macroW0[T, M[_]]: pickle.Writer[T] = macro macroWImpl[T, M]

  def macroRImpl[T, R[_]](
      c0: scala.reflect.macros.blackbox.Context
  )(implicit e1: c0.WeakTypeTag[T], e2: c0.WeakTypeTag[R[_]]): c0.Expr[R[T]] = {
    val res = new Reading[R] {
      val c: c0.type = c0
      // + upickle deviation
      override def annotate(tpe: c.Type)(
          derived: c.universe.Tree
      ): c.universe.Tree = derived
      // - upickle deviation
      def typeclass = e2
    }.derive(e1.tpe)
    c0.Expr[R[T]](res)
  }

  def macroWImpl[T, W[_]](
      c0: scala.reflect.macros.blackbox.Context
  )(implicit e1: c0.WeakTypeTag[T], e2: c0.WeakTypeTag[W[_]]): c0.Expr[W[T]] = {
    val res = new Writing[W] {
      val c: c0.type = c0
      // + upickle deviation
      override def annotate(tpe: c.Type)(
          derived: c.universe.Tree
      ): c.universe.Tree = derived
      // - upickle deviation
      def typeclass = e2
    }.derive(e1.tpe)
    c0.Expr[W[T]](res)
  }
}
