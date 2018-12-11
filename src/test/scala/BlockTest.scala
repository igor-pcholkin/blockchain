import java.time.LocalDateTime

import core.{Block, SHA256}
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
}
