import java.time.LocalDateTime

import core.{Block, BlockChain}
import org.scalatest.{FlatSpec, Matchers}

class BlockChainTest extends FlatSpec with Matchers {
  "Initial blockchain" should "contain the only origin block" in {
    val bc = new BlockChain
    bc.chain.size() shouldBe 1
    bc.chain should contain (bc.origin)
    bc.origin shouldBe Block(0, Array[Byte](), LocalDateTime.of(2018, 12, 11, 17, 40, 0), "Future is here".getBytes)
  }

  "generation of new block on original blockchain" should "produce correct block" in {
    val bc = new BlockChain
    val newBlock = bc.genNextBlock("Hi".getBytes)
    newBlock.index shouldBe 1
    newBlock.prevHash shouldBe bc.origin.hash
    newBlock.timestamp.compareTo(bc.origin.timestamp) > 0 shouldBe true
    new String(newBlock.data) shouldBe "Hi"
  }

  "getLatestBlock on original blockchain" should "return origin block" in {
    val bc = new BlockChain
    bc.getLatestBlock shouldBe bc.origin
  }
}
