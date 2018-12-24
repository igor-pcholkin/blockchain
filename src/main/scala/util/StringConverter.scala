package util

import org.apache.commons.codec.binary.Base64

trait StringConverter {
  def hexBytesStr(bytes: Array[Byte]): String = (bytes map byteToHex).mkString

  def byteToHex(byte: Byte): String = {
    val hexArray: Array[Char] = "0123456789ABCDEF".toCharArray
    "" + hexArray((byte.toInt & 0xFF) >>> 4) + hexArray(byte & 0xF)
  }

  def bytesToBase64Str(bytes: Array[Byte]): String = {
    new String(Base64.encodeBase64(bytes))
  }

  def base64StrToBytes(str: String): Array[Byte] = Base64.decodeBase64(str)
}
