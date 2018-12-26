package core

import java.time.LocalDateTime

import org.scalatest.{FlatSpec, Matchers}
import Block.CURRENT_BLOCK_VERSION

class BlockTest extends FlatSpec with Matchers {
  "overridden equals method of Block" should "work correctly" in {
    Block(CURRENT_BLOCK_VERSION, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes()) shouldBe
      Block(CURRENT_BLOCK_VERSION, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes())
    Block(CURRENT_BLOCK_VERSION, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes()) should not be
      Block((CURRENT_BLOCK_VERSION + 1).toByte, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes())
  }

  "overridden hashCode method of Block" should "work correctly" in {
    val hc1 = Block(CURRENT_BLOCK_VERSION, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes).hashCode()
    val hc2 = Block(CURRENT_BLOCK_VERSION, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes).hashCode()
    hc1 shouldBe hc2
    hc1 should not be Block((CURRENT_BLOCK_VERSION + 1).toByte, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes()).hashCode
  }

  "hash method" should "return SHA256 hash" in {
    val block = Block(CURRENT_BLOCK_VERSION, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes)
    block.hash shouldBe SHA256.hash(block)
  }

  "toString method" should "convert block to string" in {
    val block = Block(CURRENT_BLOCK_VERSION, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes)
    block.toString shouldBe "1|MTIzNA==|2018-01-24T15:00|SGk="
  }

  "Block object" should "be able to parse block" in {
    val mayBeBlock = Block.parse("1|MTIzNA==|2018-01-24T15:00|SGk=")
    mayBeBlock.get shouldBe Block(CURRENT_BLOCK_VERSION, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes)
  }

  "Block object" should "return None when parsing invalid string to block" in {
    Block.parse("1MTIzNA==|2018-01-24T15:00|SGk=") shouldBe None
    Block.parse("a|MTIzNA==|2018-01-24T15:00|SGk=") shouldBe None
    Block.parse("1|MTIzNA==|2018-01-24W15:00|SGk=") shouldBe None
  }

}
