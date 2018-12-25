package core

import java.time.LocalDateTime

import org.scalatest.{FlatSpec, Matchers}

class BlockTest extends FlatSpec with Matchers {
  "overridden equals method of Block" should "work correctly" in {
    Block(0, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes()) shouldBe
      Block(0, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes())
    Block(0, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes()) should not be
      Block(1, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes())
  }

  "overridden hashCode method of Block" should "work correctly" in {
    val hc1 = Block(0, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes).hashCode()
    val hc2 = Block(0, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes).hashCode()
    hc1 shouldBe hc2
    hc1 should not be Block(1, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes()).hashCode
  }

  "hash method" should "return SHA256 hash" in {
    val block = Block(0, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes)
    block.hash shouldBe SHA256.hash(block)
  }

  "toString method" should "convert block to string" in {
    val block = Block(0, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes)
    block.toString shouldBe "0|MTIzNA==|2018-01-24T15:00|SGk="
  }

  "Block object" should "be able to parse block" in {
    val mayBeBlock = Block.parse("0|MTIzNA==|2018-01-24T15:00|SGk=")
    mayBeBlock.get shouldBe Block(0, "1234".getBytes, LocalDateTime.of(2018, 1, 24, 15, 0, 0), "Hi".getBytes)
  }

  "Block object" should "return None when parsing invalid string to block" in {
    Block.parse("0MTIzNA==|2018-01-24T15:00|SGk=") shouldBe None
    Block.parse("a|MTIzNA==|2018-01-24T15:00|SGk=") shouldBe None
    Block.parse("0|MTIzNA==|2018-01-24W15:00|SGk=") shouldBe None
  }

}
