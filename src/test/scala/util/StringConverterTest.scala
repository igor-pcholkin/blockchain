package util

import org.scalatest.{FlatSpec, Matchers}

class StringConverterTest extends FlatSpec with Matchers with StringConverter {
  "String converter" should "be able to encode bytes and decode string to the same bytes using base64" in {
    val bytes = "123".getBytes
    val base64Encoded = bytesToBase64Str(bytes)
    base64Encoded.length > 0 shouldBe true
    base64StrToBytes(base64Encoded) shouldBe bytes
  }
}
