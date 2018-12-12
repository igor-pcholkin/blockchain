import java.time.LocalDateTime

import core.{Block, BlockChain, SHA256}
import org.scalatest.{FlatSpec, Matchers}

class BlockTest extends FlatSpec with Matchers {
  "overridden equals method of Block" should "work correctly" in {
    Block(0, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes()) shouldBe
      Block(0, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes())
    Block(0, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes()) should not be (
      Block(1, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes())
      )
  }

  "overridden hashCode method of Block" should "work correctly" in {
    val hc1 = Block(0, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes).hashCode
    val hc2 = Block(0, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes).hashCode
    hc1 shouldBe hc2
    hc1 should not be Block(1, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes()).hashCode
  }

  "hash method" should "return SHA256 hash" in {
    val block = Block(0, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes)
    block.hash shouldBe SHA256.hash(block)
  }

  "toString method" should "convert block to string" in {
    val block = Block(0, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes)
    block.toString shouldBe "0:0x310x320x330x34:2018-01-24T15:00:0x480x69"
  }

  "hexBytesStr method for origin block hash" should "produce correct hex representation of origin block hash" in {
    val bc = new BlockChain
    val origin = bc.origin
    val hash = origin.hash
    val hexStr = origin.hexBytesStr(hash)
    hexStr shouldBe "0x710x0F0x7E0xA50x0A0x4C0xEA0xA80xE00xA80xD00x420xD20x220x3A0x200xD60x780x2E0xA00xF30xD40xC20xEC0x220xC50x970x0F0x4D0x2E0x620x26"
  }
}
