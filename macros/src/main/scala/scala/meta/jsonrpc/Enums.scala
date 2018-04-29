package scala.meta.jsonrpc

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object Enums {

  /**
   * Macro to collect subtypes of T inside the companion object of T.
   *
   * Example usage:
   * {{{
   *   sealed abstract class Weekday
   *   object Weekday {
   *
   *     case object Monday extends Weekday
   *     case object Tuesday extends Weekday
   *     case object Rest extends Weekday
   *
   *     val values: IndexedSeq[Weekday] = Enums.findValues[Weekday]
   *     // Expands into
   *     // val values: IndexedSeq[Weekday] = IndexedSeq(Monday, Tuesday, Rest)
   *   }
   * }}}
   *
   */
  def findValues[T]: IndexedSeq[T] = macro impl[T]

  def impl[T: c.WeakTypeTag](c: blackbox.Context): c.Tree = {
    import c.universe._
    val T = weakTypeOf[T]
    var members: List[Ident] = Nil
    T.companion.members.foreach { sym =>
      if (sym.isModule && sym.info <:< T) {
        members ::= Ident(sym)
      }
    }
    q"_root_.scala.IndexedSeq(..$members)"
  }

}
