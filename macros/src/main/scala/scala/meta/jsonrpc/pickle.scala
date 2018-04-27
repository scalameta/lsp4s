package scala.meta.jsonrpc

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
}
