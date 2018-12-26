package core

import java.time.LocalDateTime

import org.mockito.Mockito.{times, verify, when}
import org.scalatest
import org.scalatest.FlatSpec
import org.mockito.Matchers
import Block.CURRENT_BLOCK_VERSION

class BlockChainTest extends FlatSpec with scalatest.Matchers {
  "Initial blockchain" should "contain the only origin block" in {
    val bc = new TestBlockChain
    bc.chain.size() shouldBe 1
    bc.chain should contain (bc.origin)
    bc.origin shouldBe Block(CURRENT_BLOCK_VERSION, Array[Byte](), LocalDateTime.of(2018, 12, 11, 17, 40, 0), "Future is here".getBytes)
  }

  "generation of new block on original blockchain" should "produce correct block" in {
    val bc = new TestBlockChain
    val newBlock = bc.genNextBlock("Hi".getBytes)
    newBlock.version shouldBe 1
    newBlock.prevHash shouldBe bc.origin.hash
    newBlock.timestamp.compareTo(bc.origin.timestamp) > 0 shouldBe true
    new String(newBlock.data) shouldBe "Hi"
  }

  "getLatestBlock on original blockchain" should "return origin block" in {
    val bc = new TestBlockChain
    bc.getLatestBlock shouldBe bc.origin
  }

  "adding new generated block" should "go without problems" in {
    val bc = new TestBlockChain
    val newBlock = bc.genNextBlock("Fund transfer from A to B".getBytes)
    bc.add(newBlock)
    bc.chain.size shouldBe 2
    bc.getLatestBlock shouldBe newBlock
  }

  "newly generated block with incorrect version" should "not be added" in {
    val bc = new TestBlockChain
    val newBlock = bc.genNextBlock("Fund transfer from A to B".getBytes)
    val newBlockInvIdx = Block((CURRENT_BLOCK_VERSION + 1).toByte, newBlock.prevHash, newBlock.timestamp, newBlock.data)
    bc.add(newBlockInvIdx)

    val newBlockInvIdx2 = Block(0, newBlock.prevHash, newBlock.timestamp, newBlock.data)
    bc.add(newBlockInvIdx2)

    val newBlock4 = Block(-2, newBlock.prevHash, newBlock.timestamp, newBlock.data)
    bc.add(newBlock4)

    bc.chain.size shouldBe 1
    bc.getLatestBlock shouldBe bc.origin
  }

  "newly generated block with incorrect hash of previous block" should "not be added" in {
    val bc = new TestBlockChain
    val newBlock = bc.genNextBlock("Fund transfer from A to B".getBytes)
    val newBlockIncPrevHash = Block(CURRENT_BLOCK_VERSION, Array[Byte](1.toByte), newBlock.timestamp, newBlock.data)
    bc.add(newBlockIncPrevHash)
    bc.chain.size shouldBe 1
    bc.getLatestBlock shouldBe bc.origin
  }

  "newly generated block with timestamp less than timestamp of previous block" should "not be added" in {
    val bc = new TestBlockChain
    val newBlock = bc.genNextBlock("Fund transfer from A to B".getBytes)
    val newBlockIncTimestamp = Block(CURRENT_BLOCK_VERSION, newBlock.prevHash, LocalDateTime.of(2018, 12, 10, 23, 0, 0), newBlock.data)
    bc.add(newBlockIncTimestamp)
    bc.chain.size shouldBe 1
    bc.getLatestBlock shouldBe bc.origin
  }

  "serialize method on blockchain consisting of two blocks" should "produce a correct sequence of bytes" in {
    val bc = new TestBlockChain
    val newBlock = Block(CURRENT_BLOCK_VERSION, bc.getLatestBlock.hash, LocalDateTime.of(2018, 12, 24, 15, 0, 0), "Hi".getBytes)
    bc.add(newBlock)
    bc.chain.size() shouldBe 2
    bc.serialize shouldBe ("1||2018-12-11T17:40|RnV0dXJlIGlzIGhlcmU=," +
      "1|OHLfWn7tKNZEBkLgRnrA3vbFr6zO136VHP1sJM/051c=|2018-12-24T15:00|SGk=")
  }

  "reading blockchain from file system" should "add new blocks to blockchain" in {
    val bc = new TestBlockChain

    bc.chain.size() shouldBe 1

    val block1 = Block(CURRENT_BLOCK_VERSION, bc.getLatestBlock.hash, LocalDateTime.of(2018, 12, 24, 15, 0, 0), "Hi".getBytes)
    val block2 = Block(CURRENT_BLOCK_VERSION, block1.hash, LocalDateTime.of(2018, 12, 24, 16, 0, 0), "Hi again".getBytes)

    when(bc.chainFileOps.getChainDir("Riga")).thenReturn("chaindir")
    when(bc.chainFileOps.isChainDirExists("Riga")).thenReturn(true)
    when(bc.chainFileOps.readBlock(1, "chaindir")).thenReturn(Some(block1))
    when(bc.chainFileOps.readBlock(2, "chaindir")).thenReturn(Some(block2))
    when(bc.chainFileOps.readBlock(3, "chaindir")).thenReturn(None)

    bc.readChain()

    bc.chain.size() shouldBe 3
    val blocks = bc.chain.toArray
    blocks(0) shouldBe bc.origin
    blocks(1) shouldBe block1
    blocks(2) shouldBe block2
  }

  "reading blockchain from file system" should "not add new blocks to blockchain if it is not exist on disk" in {
    val bc = new TestBlockChain

    bc.chain.size() shouldBe 1
    when(bc.chainFileOps.getChainDir("Riga")).thenReturn("chaindir")
    when(bc.chainFileOps.isChainDirExists("Riga")).thenReturn(false)
    bc.readChain()

    bc.chain.size() shouldBe 1
  }

  "writing blockchain to disk" should "create blockchain directory if it is not exists" in {
    val bc = new TestBlockChain

    when(bc.chainFileOps.getChainDir("Riga")).thenReturn("chaindir")
    when(bc.chainFileOps.isChainDirExists("Riga")).thenReturn(false)

    bc.writeChain()

    verify(bc.chainFileOps, times(1)).createChainDir(Matchers.eq("Riga"))
  }

  "writing blockchain to disk" should "create write all its blocks" in {
    val bc = new TestBlockChain

    val block1 = Block(CURRENT_BLOCK_VERSION, bc.getLatestBlock.hash, LocalDateTime.of(2018, 12, 24, 15, 0, 0), "Hi".getBytes)
    val block2 = Block(CURRENT_BLOCK_VERSION, block1.hash, LocalDateTime.of(2018, 12, 24, 16, 0, 0), "Hi again".getBytes)
    bc.add(block1)
    bc.add(block2)

    bc.chain.size() shouldBe 3

    when(bc.chainFileOps.getChainDir("Riga")).thenReturn("chaindir")
    when(bc.chainFileOps.isChainDirExists("Riga")).thenReturn(true)

    bc.writeChain()

    verify(bc.chainFileOps, times(1)).writeBlock(Matchers.eq(0), Matchers.eq(bc.origin), Matchers.eq("chaindir"))
    verify(bc.chainFileOps, times(1)).writeBlock(Matchers.eq(1), Matchers.eq(block1), Matchers.eq("chaindir"))
    verify(bc.chainFileOps, times(1)).writeBlock(Matchers.eq(2), Matchers.eq(block2), Matchers.eq("chaindir"))
  }

}
