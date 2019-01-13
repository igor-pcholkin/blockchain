package core

import java.time.LocalDateTime

import messages.SignedStatementMessage

import json.JsonSerializer
import json.FactJson._

import scala.collection.mutable.ListBuffer

class ProdBlockChain(nodeName: String) extends BlockChain(nodeName) {

  readChain()

  override def chainFileOps: ChainFileOps = ProdChainFileOps
}

abstract class BlockChain(nodeName: String) {
  val origin = Block(Block.CURRENT_BLOCK_VERSION, Array[Byte](), LocalDateTime.of(2018, 12, 11, 17, 40, 0), "Future is here".getBytes)
  private val chain = ListBuffer[Block]()
  chain.append(origin)

  def chainFileOps: ChainFileOps

  def size: Int = chain.size

  def blocksFrom(fromBlockNo: Int): ListBuffer[Block] = {
    chain.drop(fromBlockNo)
  }

  def genNextBlock(data: Array[Byte]): Block = {
    val prevBlock = getLatestBlock
    val prevHash = SHA256.hash(prevBlock)
    val nextTimestamp = LocalDateTime.now()
    Block(Block.CURRENT_BLOCK_VERSION, prevHash, nextTimestamp, data)
  }

  def add(block: Block): Unit = synchronized {
    if (isValid(block))
      chain.append(block)
  }

  def isValid(block: Block): Boolean = {
    val latestBlock = getLatestBlock
    block.version <= Block.CURRENT_BLOCK_VERSION && block.version > 0 && block.prevHash.toSeq == latestBlock.hash.toSeq &&
      block.timestamp.compareTo(latestBlock.timestamp) >= 0
  }

  def getLatestBlock: Block = chain.last

  def serialize: String = {
    val sb = new StringBuilder
    val it = chain.iterator
    while (it.hasNext) {
      sb.append(it.next)
      if (it.hasNext)
        sb.append(",")
    }
    sb.toString
  }

  def writeChain(): Unit = synchronized {
    val chainDir = chainFileOps.getChainDir(nodeName)
    if (!chainFileOps.isChainDirExists(nodeName)) {
      chainFileOps.createChainDir(nodeName)
    }

    chain.foldLeft(0) { (i, block) =>
      chainFileOps.writeBlock(i, block, chainDir)
      i + 1
    }
  }

  def readChain(): Unit = synchronized {
    val chainDir = chainFileOps.getChainDir(nodeName)
    if (!chainFileOps.isChainDirExists(nodeName)) {
      None
    } else {
      Stream.from(chain.size).map { i =>
        val mayBeBlock = chainFileOps.readBlock(i, chainDir)
        mayBeBlock foreach { block =>
          add(block)
        }
        mayBeBlock
      }.takeWhile(_.nonEmpty).toList
    }
  }

  def addFactToNewBlock(signedStatement: SignedStatementMessage): Unit = {
    val fact = Fact(signedStatement.statement, signedStatement.providedSignaturesForKeys)
    val serializedFact = JsonSerializer.serialize(fact).getBytes
    val newBlock = genNextBlock(serializedFact)
    add(newBlock)
    writeChain()
  }


}
