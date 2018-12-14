package util

trait Convert {
  def hexBytesStr(bytes: Array[Byte]) = {
    bytes map (byteToHex(_)) mkString
  }

  def byteToHex(byte: Byte) = {
    val hexArray: Array[Char] = "0123456789ABCDEF".toCharArray
    "" + hexArray((byte.toInt & 0xFF) >>> 4) + hexArray(byte & 0xF)
  }

}
