package core

import org.scalatest.{FlatSpec, Matchers}
import util.StringConverter

class SHA256Test extends FlatSpec with Matchers with StringConverter {
  "SHA256" should "calculate hashes for two different messages and ensure those are different" in {
    println(SHA256.hash("123").length)
    println(SHA256.hash("1234").length)
    val hash1 = SHA256.hash("123")
    val hash2 = SHA256.hash("1234")
    println(hexBytesStr(hash1))
    println(hexBytesStr(hash2))
    hash1.toSeq should not be hash2.toSeq
  }
}
