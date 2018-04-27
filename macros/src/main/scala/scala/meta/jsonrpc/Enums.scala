package scala.meta.jsonrpc

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object Enums {

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
