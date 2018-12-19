package core

import keys.{KeysFileOps, KeysGenerator, KeysSerializator}
import org.mockito.Mockito.when
import org.scalatest._
import org.scalatest.mockito.MockitoSugar

class SignerTest extends FlatSpec with Matchers with KeysGenerator with MockitoSugar with KeysSerializator {
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

  "Signer" should "sign with signature related to given user" in {
    val signer = new Signer(keysFileOps)
    val serializedPublicKey = serialize(pair.getPublic)
    val serializedPrivateKey = serialize(pair.getPrivate)
    when(keysFileOps.readKeyFromFile("Igor/publicKey")).thenReturn(serializedPublicKey)
    when(keysFileOps.readKeyFromFile("Igor/privateKey")).thenReturn(serializedPrivateKey)
    val data = "123".getBytes
    val signature  = signer.sign("Igor", serializedPublicKey, data)
    Signer.verify(signature, data, pair.getPublic) shouldBe true
  }

  "Signer" should "refuse to sign with signature not related to given user" in {
    val signer = new Signer(keysFileOps)
    val serializedPublicKey = serialize(pair.getPublic)
    val serializedPrivateKey = serialize(pair.getPrivate)
    when(keysFileOps.readKeyFromFile("Igor/publicKey")).thenReturn(serializedPublicKey)
    when(keysFileOps.readKeyFromFile("Igor/privateKey")).thenReturn(serializedPrivateKey)
    val data = "123".getBytes
    assertThrows[RuntimeException] {
        signer.sign("Igor", serialize(pair2.getPublic), data)
    }
  }

  import java.security.KeyPair
  val pair: KeyPair = generateKeyPair()
  val pair2: KeyPair = generateKeyPair()
  override val keysFileOps = mock[KeysFileOps]
}
