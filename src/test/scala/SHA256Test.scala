import core.SHA256
import org.scalatest.{FlatSpec, Matchers}

class SHA256Test extends FlatSpec with Matchers {
  "SHA256" should "calculate hashes for two different messages and ensure those are different" in {
    println(SHA256.hash("123").length)
    println(SHA256.hash("1234").length)
    val hash1 = new String(SHA256.hash("123"))
    val hash2 = new String(SHA256.hash("1234"))
    println(hash1)
    println(hash2)
    hash1 should not be(hash2)
  }
}
