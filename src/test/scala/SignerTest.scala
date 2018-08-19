import java.security.SecureRandom

import core.Signer
import org.scalatest._

class SignerTest extends FlatSpec with Matchers {
  "Signer" should "verify signature with corresponding public key" in {
    val privateKey = pair.getPrivate
    val publicKey = pair.getPublic
    val data = "123".getBytes
    val signature  = Signer.sign(privateKey, data)
    Signer.verify(signature, data, publicKey) shouldBe true
  }

  "Signer" should "verify signature with alternative public key" in {
    val privateKey = pair.getPrivate
    val publicKey = pair2.getPublic
    val data = "123".getBytes
    val signature  = Signer.sign(privateKey, data)
    Signer.verify(signature, data, publicKey) shouldBe false
  }

  import java.security.KeyPair
  import java.security.KeyPairGenerator

  val keyGen: KeyPairGenerator = KeyPairGenerator.getInstance("EC")
  keyGen.initialize(256, new SecureRandom())
  val pair: KeyPair = keyGen.generateKeyPair

  println(s"Keys1: Private: ${pair.getPrivate}, public: ${pair.getPublic}")

  val keyGen2: KeyPairGenerator = KeyPairGenerator.getInstance("EC")
  keyGen2.initialize(256, new SecureRandom())
  val pair2: KeyPair = keyGen.generateKeyPair

  println(s"Keys2: Private: ${pair2.getPrivate}, public: ${pair2.getPublic}")
}
