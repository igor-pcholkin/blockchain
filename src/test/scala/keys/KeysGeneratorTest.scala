package keys

import org.scalatest.{FlatSpec, Matchers}

class KeysGeneratorTest extends FlatSpec with Matchers with KeysGenerator {
  "key generator" should "produce key pair" in {
    val pair = generateKeyPair()
    pair.getPrivate should not be null
    pair.getPublic should not be null
  }
}
