package core

import java.time.LocalDateTime

import util.Convert

case class Block(index: Int, prevHash: Array[Byte], timestamp: LocalDateTime, data: Array[Byte]) extends Convert {
  override def equals(other: Any) = {
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

  def hash = SHA256.hash(this)

  override def toString() = {
    s"$index:${hexBytesStr(prevHash)}:$timestamp:${hexBytesStr(data)}"
  }

}
