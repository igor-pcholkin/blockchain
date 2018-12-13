package keys

import java.security.{KeyPair, KeyPairGenerator, SecureRandom}

trait KeysGenerator {
  def generateKeyPair(): KeyPair = {
    val keyGen: KeyPairGenerator = KeyPairGenerator.getInstance("EC")
    keyGen.initialize(256, new SecureRandom())
    keyGen.generateKeyPair
  }
}
