package scala.meta.jsonrpc

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util
import monix.reactive.Observable
import scribe.LoggerSupport
import ujson.Js
import scala.meta.jsonrpc.pickle.write
import scala.meta.internal.jsonrpc._

final class BaseProtocolMessage(
    val header: Map[String, String],
    val content: Array[Byte]
) {

  override def equals(obj: scala.Any): Boolean =
    this.eq(obj.asInstanceOf[Object]) || {
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
    fromJson(msg.asJsonEncoded)
  def fromJson(json: Js): BaseProtocolMessage =
    fromBytes(write(json).getBytes(StandardCharsets.UTF_8))
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
