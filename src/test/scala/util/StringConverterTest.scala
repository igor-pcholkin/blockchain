package util

import core.BlockChain
import org.scalatest.{FlatSpec, Matchers}

class StringConverterTest extends FlatSpec with Matchers with StringConverter {
  "hexBytesStr method for origin block hash" should "produce correct hex representation of origin block hash" in {
    val bc = new BlockChain
    val origin = bc.origin
    val hash = origin.hash
    val hexStr = hexBytesStr(hash)
    hexStr shouldBe "710F7EA50A4CEAA8E0A8D042D2223A20D6782EA0F3D4C2EC22C5970F4D2E6226"
  }

}
