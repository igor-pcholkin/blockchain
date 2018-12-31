package core

import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedDeque

import messages.SignedStatementMessage

import scala.collection.JavaConverters._

class ProdBlockChain(nodeName: String) extends BlockChain(nodeName) {

  readChain()

  override def chainFileOps: ChainFileOps = ProdChainFileOps
}

abstract class BlockChain(nodeName: String) {
  val origin = Block(Block.CURRENT_BLOCK_VERSION, Array[Byte](), LocalDateTime.of(2018, 12, 11, 17, 40, 0), "Future is here".getBytes)
  val chain = new ConcurrentLinkedDeque[Block]()
  chain.add(origin)

  def chainFileOps: ChainFileOps

  def genNextBlock(data: Array[Byte]): Block = {
    val prevBlock = getLatestBlock
    val prevHash = SHA256.hash(prevBlock)
    val nextTimestamp = LocalDateTime.now()
    Block(Block.CURRENT_BLOCK_VERSION, prevHash, nextTimestamp, data)
  }

  def add(block: Block): Unit = {
    if (isValid(block))
      chain.add(block)
  }

  def isValid(block: Block): Boolean = {
    block.version <= Block.CURRENT_BLOCK_VERSION && block.version > 0 && block.prevHash.toSeq == getLatestBlock.hash.toSeq &&
      block.timestamp.compareTo(getLatestBlock.timestamp) > 0
  }

  def getLatestBlock: Block = chain.peekLast()

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

  def writeChain(): Unit = {
    val chainDir = chainFileOps.getChainDir(nodeName)
    if (!chainFileOps.isChainDirExists(nodeName)) {
      chainFileOps.createChainDir(nodeName)
    }

    val it = chain.iterator.asScala
    it.foldLeft(0) { (i, block) =>
      chainFileOps.writeBlock(i, block, chainDir)
      i + 1
    }
  }

  def readChain(): Unit = {
    val chainDir = chainFileOps.getChainDir(nodeName)
    if (!chainFileOps.isChainDirExists(nodeName)) {
      None
    } else {
      Stream.from(chain.size()).map { i =>
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
    val serializedFact = Serializator.serialize(fact)(Fact.encoder).getBytes
    val newBlock = genNextBlock(serializedFact)
    add(newBlock)
  }


}
