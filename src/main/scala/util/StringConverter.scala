package util

import org.apache.commons.codec.binary.Base64

trait StringConverter {
  def bytesToBase64Str(bytes: Array[Byte]): String = {
    new String(Base64.encodeBase64(bytes))
  }

  def base64StrToBytes(str: String): Array[Byte] = Base64.decodeBase64(str)
}
