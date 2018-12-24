package core

import java.time.LocalDateTime

import util.StringConverter

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
    s"$index:${hexBytesStr(prevHash)}:$timestamp:${hexBytesStr(data)}"
  }

}
