package core

import java.time.LocalDateTime

import util.StringConverter

import scala.util.Try

object Block extends StringConverter {
  def parse(s: String): Option[Block] = {
    val fields = s.split("\\|")
    if (fields.length == 4) {
      Try {
        val index = fields(0).toInt
        val prevHash = base64StrToBytes(fields(1))
        val timestamp = LocalDateTime.parse(fields(2))
        val data = base64StrToBytes(fields(3))
        Some(Block(index, prevHash, timestamp, data))
      } getOrElse None
    } else {
      None
    }
  }
}

case class Block(index: Int, prevHash: Array[Byte], timestamp: LocalDateTime, data: Array[Byte]) extends StringConverter {
  override def equals(other: Any): Boolean = {
    if (!other.isInstanceOf[Block])
      false
    else {
      val otherAsBlock = other.asInstanceOf[Block]
      index == otherAsBlock.index && prevHash.toSeq == otherAsBlock.prevHash.toSeq && timestamp == otherAsBlock.timestamp &&
        data.toSeq == otherAsBlock.data.toSeq
    }
  }

  override def hashCode(): Int = {
    SHA256.hash(this).toSeq.hashCode()
  }

  lazy val hash: Array[Byte] = SHA256.hash(this)

  override def toString: String = {
    s"$index|${bytesToBase64Str(prevHash)}|$timestamp|${bytesToBase64Str(data)}"
  }

}
