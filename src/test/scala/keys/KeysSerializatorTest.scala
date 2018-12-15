package keys

import java.security.{KeyPair, KeyPairGenerator, SecureRandom}

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class KeysSerializatorTest extends FlatSpec with Matchers with KeysSerializator with MockitoSugar {
  "private key serialization/deserialization" should "work correctly" in {
    val keyGen: KeyPairGenerator = KeyPairGenerator.getInstance("EC")
    keyGen.initialize(256, new SecureRandom())
    val pair: KeyPair = keyGen.generateKeyPair
    val privateKey = pair.getPrivate
    val publicKey = pair.getPublic
    val privateKeySerialized = serialize(privateKey)
    val publicKeySerialized = serialize(publicKey)

    val privateKeyDeserialized = deserializePrivate(privateKeySerialized)
    privateKey shouldBe privateKeyDeserialized

    val publicKeyDeserialized = deserializePublic(publicKeySerialized)
    publicKey shouldBe publicKeyDeserialized
  }

  val keysFileOps = mock[KeysFileOps]
}
