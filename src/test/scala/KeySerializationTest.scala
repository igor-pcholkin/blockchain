import java.security.{KeyPair, KeyPairGenerator, SecureRandom}
import keys.Serialization
import org.scalatest.{FlatSpec, Matchers}

class KeySerializationTest extends FlatSpec with Matchers {
  "private key serialization/deserialization" should "work correctly" in {
    val keyGen: KeyPairGenerator = KeyPairGenerator.getInstance("EC")
    keyGen.initialize(256, new SecureRandom())
    val pair: KeyPair = keyGen.generateKeyPair
    val privateKey = pair.getPrivate
    val publicKey = pair.getPublic
    val privateKeySerialized = Serialization.serialize(privateKey)
    val publicKeySerialized = Serialization.serialize(publicKey)

    val privateKeyDeserialized = Serialization.deserializePrivate(privateKeySerialized)
    privateKey shouldBe privateKeyDeserialized

    val publicKeyDeserialized = Serialization.deserializePublic(publicKeySerialized)
    publicKey shouldBe publicKeyDeserialized
  }
}
