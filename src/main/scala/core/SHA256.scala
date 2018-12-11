package core

import java.security.MessageDigest


object SHA256 {

  val digest = MessageDigest.getInstance("SHA-256")
  def hash(str: String): Array[Byte] = digest.digest(str.getBytes)
  def hash(block: Block): Array[Byte] = hash(block.index.toString + block.prevHash.toString + block.timestamp.toString + block.data)
}
