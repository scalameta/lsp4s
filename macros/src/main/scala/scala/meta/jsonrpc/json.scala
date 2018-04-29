package scala.meta.jsonrpc

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * Macro annotation that injects in implicit upickle ReadWriter in the companion object.
 *
 * The expansion is roughly the following
 *
 * {{{
 *   // before
 *   @json case class User(name: String, age: Int)
 *   // after
 *   case class User(name: String, age: Int)
 *   object User {
 *     implicit val rw: ReadWriter[User] = macroRW[User]
 *   }
 * }}}
 */
final class json extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro JsonCodecExpander.expand
}

final class JsonCodecExpander(val c: blackbox.Context) {
  import c.universe._
  def rw(name: TypeName) =
    q"""
      implicit val rw: _root_.scala.meta.jsonrpc.pickle.ReadWriter[$name] =
        _root_.scala.meta.jsonrpc.pickle.macroRW[$name]
     """
  def expand(annottees: Tree*): Tree = annottees match {
    case List(c: ClassDef) if c.mods.hasFlag(Flag.CASE) =>
      q"""
         $c
         object ${c.name.toTermName} {
           ${rw(c.name)}
         }
       """
    case List(
        c: ClassDef,
        q"object $name extends { ..$early } with ..$parents { $self => ..$stats }"
        ) if c.mods.hasFlag(Flag.CASE) =>
      q"""
         $c
         object $name extends { ..$early } with ..$parents { $self =>
           ${rw(c.name)}
           ..$stats
         }
       """
    case _ =>
      c.abort(c.enclosingPosition, s"Expected case class, obtained $annottees")

  }
}
