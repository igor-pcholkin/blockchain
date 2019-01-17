package core

import java.time.LocalDateTime

import util.StringConverter

import scala.util.Try

object Block extends StringConverter {
  val NUM_BLOCK_FIELDS = 4
  val CURRENT_BLOCK_VERSION: Byte = 1

  def parse(s: String): Option[Block] = {
    val fields = s.split("\\|")
    if (fields.length == NUM_BLOCK_FIELDS) {
      Try {
        val version = fields(0).toByte
        val prevHash = base64StrToBytes(fields(1))
        val timestamp = LocalDateTime.parse(fields(2))
        val data = base64StrToBytes(fields(3))
        Some(Block(version, prevHash, timestamp, data))
      } getOrElse None
    } else {
      None
    }
  }
}

case class Block(version: Byte, prevHash: Array[Byte], timestamp: LocalDateTime, data: Array[Byte]) extends StringConverter {
  override def equals(other: Any): Boolean = {
    if (!other.isInstanceOf[Block])
      false
    else {
      val otherAsBlock = other.asInstanceOf[Block]
      version == otherAsBlock.version && prevHash.toSeq == otherAsBlock.prevHash.toSeq && timestamp == otherAsBlock.timestamp &&
        data.toSeq == otherAsBlock.data.toSeq
    }
  }

  override def hashCode(): Int = {
    SHA256.hash(this).toSeq.hashCode()
  }

  lazy val hash: Array[Byte] = SHA256.hash(this)

  override def toString: String = {
    s"$version|${bytesToBase64Str(prevHash)}|$timestamp|${bytesToBase64Str(data)}"
  }

  def isNewerThan(anotherBlock: Block) = timestamp.compareTo(anotherBlock.timestamp) > 0
}
