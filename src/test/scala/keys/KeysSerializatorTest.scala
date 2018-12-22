package keys

import java.security.KeyPair

import org.mockito.Matchers
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.FlatSpec

class KeysSerializatorTest extends FlatSpec with org.scalatest.Matchers with KeysSerializator with MockitoSugar with KeysGenerator {
  "private key serialization/deserialization" should "work correctly" in {
    val pair: KeyPair = generateKeyPair
    val privateKey = pair.getPrivate
    val publicKey = pair.getPublic
    val privateKeySerialized = serialize(privateKey)
    val publicKeySerialized = serialize(publicKey)

    val privateKeyDeserialized = deserializePrivate(privateKeySerialized)
    privateKey shouldBe privateKeyDeserialized

    val publicKeyDeserialized = deserializePublic(publicKeySerialized)
    publicKey shouldBe publicKeyDeserialized
  }

  "keys" should "be written using mock serializator" in {
    val pair: KeyPair = generateKeyPair
    val privateKey = pair.getPrivate
    val publicKey = pair.getPublic

    writeKey("Riga", privateKey)
    verify(keysFileOps, times(1)).writeKey(Matchers.eq("keys/Riga/privateKey"), Matchers.any[String])

    writeKey("Riga", publicKey)
    verify(keysFileOps, times(1)).writeKey(Matchers.eq("keys/Riga/publicKey"), Matchers.any[String])
  }

  "keys" should "be read using mock serializator" in {
    val pair: KeyPair = generateKeyPair
    val privateKey = pair.getPrivate
    val publicKey = pair.getPublic

    when(keysFileOps.readKeyFromFile(Matchers.eq("keys/Riga/privateKey"))).thenReturn(serialize(privateKey))
    readPrivateKey("Riga") shouldBe privateKey

    when(keysFileOps.readKeyFromFile(Matchers.eq("keys/Riga/publicKey"))).thenReturn(serialize(publicKey))
    readPublicKey("Riga") shouldBe publicKey
  }

  val keysFileOps = mock[KeysFileOps]
}
