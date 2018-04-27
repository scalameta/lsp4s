package scala.meta.jsonrpc

import scala.meta.jsonrpc.pickle._

/** Replacement for enumeratum IntEnum with baked-in support for upickle. */
trait IntEnum {
  def value: Int
}

trait IntEnumCompanion[A <: IntEnum] {
  def values: IndexedSeq[A]
  implicit val rw: ReadWriter[A] = readwriter[Int].bimap(
    _.value, { id =>
      values.find(_.value == id).getOrElse {
        throw new IllegalArgumentException(id.toString)
      }
    }
  )
}
