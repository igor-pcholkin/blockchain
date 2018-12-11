import core.SHA256
import org.scalatest.{FlatSpec, Matchers}

class SHA256Test extends FlatSpec with Matchers {
  "SHA256" should "calculate hashes for two different messages and ensure those are different" in {
    val hash1 = new String(SHA256.hash("123"))
    val hash2 = new String(SHA256.hash("1234"))
    println(hash1)
    println(hash2)
    hash1 should not be(hash2)
  }
}
