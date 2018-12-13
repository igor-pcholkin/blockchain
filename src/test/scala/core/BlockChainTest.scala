package core

import java.time.LocalDateTime

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

  "adding new generated block" should "go without problems" in {
    val bc = new BlockChain
    val newBlock = bc.genNextBlock("Fund transfer from A to B".getBytes)
    bc.add(newBlock)
    bc.chain.size shouldBe 2
    bc.getLatestBlock shouldBe newBlock
  }

  "newly generated block with incorrect index" should "not be added" in {
    val bc = new BlockChain
    val newBlock = bc.genNextBlock("Fund transfer from A to B".getBytes)
    val newBlockInvIdx = Block(0, newBlock.prevHash, newBlock.timestamp, newBlock.data)
    bc.add(newBlockInvIdx)
    bc.chain.size shouldBe 1
    bc.getLatestBlock shouldBe bc.origin

    val newBlockInvIdx2 = Block(5, newBlock.prevHash, newBlock.timestamp, newBlock.data)
    bc.add(newBlockInvIdx2)
    bc.chain.size shouldBe 1
    bc.getLatestBlock shouldBe bc.origin

    val newBlock4 = Block(1, newBlock.prevHash, newBlock.timestamp, newBlock.data)
    bc.add(newBlock4)
    bc.chain.size shouldBe 2
    bc.getLatestBlock shouldBe newBlock4
  }

  "newly generated block with incorrect hash of previous block" should "not be added" in {
    val bc = new BlockChain
    val newBlock = bc.genNextBlock("Fund transfer from A to B".getBytes)
    val newBlockIncPrevHash = Block(1, Array[Byte](1.toByte), newBlock.timestamp, newBlock.data)
    bc.add(newBlockIncPrevHash)
    bc.chain.size shouldBe 1
    bc.getLatestBlock shouldBe bc.origin
  }

  "newly generated block with timestamp less than timestamp of previous block" should "not be added" in {
    val bc = new BlockChain
    val newBlock = bc.genNextBlock("Fund transfer from A to B".getBytes)
    val newBlockIncTimestamp = Block(1, newBlock.prevHash, LocalDateTime.of(2018, 12, 10, 23, 0, 0), newBlock.data)
    bc.add(newBlockIncTimestamp)
    bc.chain.size shouldBe 1
    bc.getLatestBlock shouldBe bc.origin
  }

  "serialize method on blockchain consisting of two blocks" should "produce a correct sequence of bytes" in {
    val bc = new BlockChain
    val newBlock = Block(1, bc.getLatestBlock.hash, LocalDateTime.of(2018, 12, 24, 15, 0, 0), "Hi".getBytes)
    bc.add(newBlock)
    bc.chain.size() shouldBe 2
    bc.serialize shouldBe ("0::2018-12-11T17:40:4675747572652069732068657265," +
      "1:710F7EA50A4CEAA8E0A8D042D2223A20D6782EA0F3D4C2EC22C5970F4D2E6226:2018-12-24T15:00:4869").getBytes
  }
}
