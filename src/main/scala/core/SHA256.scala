package core

import java.security.MessageDigest


object SHA256 {

  val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
  def hash(str: String): Array[Byte] = digest.digest(str.getBytes)
  def hash(bytes: Array[Byte]): Array[Byte] = digest.digest(bytes)
  def hash(block: Block): Array[Byte] = hash(block.version.toString + block.prevHash.toSeq.toString +
    block.timestamp.toString + block.data.toSeq.toString)
}
