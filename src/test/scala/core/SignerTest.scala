package core

import keys.KeysGenerator
import org.scalatest._

class SignerTest extends FlatSpec with Matchers with KeysGenerator {
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
  val pair: KeyPair = generateKeyPair()

  println(s"Keys1: Private: ${pair.getPrivate}, public: ${pair.getPublic}")

  val pair2: KeyPair = generateKeyPair()

  println(s"Keys2: Private: ${pair2.getPrivate}, public: ${pair2.getPublic}")
}
