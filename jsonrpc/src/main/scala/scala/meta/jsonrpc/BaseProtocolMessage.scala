package scala.meta.jsonrpc

import io.circe.syntax._
import io.circe.Json
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util
import monix.reactive.Observable
import scribe.LoggerSupport
import scala.meta.internal.jsonrpc._

/** A single raw un-parsed request, response or notification */
final class BaseProtocolMessage(
    val header: Map[String, String],
    val content: Array[Byte]
) {

  override def equals(obj: Any): Boolean =
    this.eq(obj.asInstanceOf[AnyRef]) || {
      obj match {
        case m: BaseProtocolMessage =>
          header.equals(m.header) &&
            util.Arrays.equals(content, m.content)
      }
    }

  override def toString: String = {
    val bytes = MessageWriter.write(this)
    StandardCharsets.UTF_8.decode(bytes).toString
  }
}

object BaseProtocolMessage {
  val ContentLen = "Content-Length"

  def apply(msg: Message): BaseProtocolMessage =
    fromJson(msg.asJson)
  def fromJson(json: Json): BaseProtocolMessage =
    fromBytes(json.noSpaces.getBytes(StandardCharsets.UTF_8))
  def fromBytes(bytes: Array[Byte]): BaseProtocolMessage =
    new BaseProtocolMessage(
      Map("Content-Length" -> bytes.length.toString),
      bytes
    )

  def fromInputStream(
      in: InputStream,
      logger: LoggerSupport
  ): Observable[BaseProtocolMessage] =
    fromBytes(Observable.fromInputStream(in), logger)

  def fromBytes(
      in: Observable[Array[Byte]],
      logger: LoggerSupport
  ): Observable[BaseProtocolMessage] =
    fromByteBuffers(in.map(ByteBuffer.wrap), logger)

  def fromByteBuffers(
      in: Observable[ByteBuffer],
      logger: LoggerSupport
  ): Observable[BaseProtocolMessage] =
    in.executeWithFork.liftByOperator(new BaseProtocolMessageParser(logger))
}
