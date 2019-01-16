package core

import java.time.LocalDateTime

import messages.SignedStatementMessage
import json.{FactJson, JsonSerializer}
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

  def blocksFrom(fromBlockNo: Int): ListBuffer[Block] = chain.drop(fromBlockNo)

  def takeN(n: Int): Unit = chain.remove(n, size - n)

  def blockAt(i: Int): Block = blocksFrom(i).head

  def genNextBlock(data: Array[Byte], timestamp: LocalDateTime = LocalDateTime.now()): Block = {
    val prevBlock = getLatestBlock
    val prevHash = SHA256.hash(prevBlock)
    Block(Block.CURRENT_BLOCK_VERSION, prevHash, timestamp, data)
  }

  def add(block: Block): Unit = synchronized {
    if (isValid(block, size))
      chain.append(block)
  }

  def isValid(block: Block, i: Int = size): Boolean = size >= i && i > 0 && isValidWithPrevBlock(block, blockAt(i - 1))

  private def isValidWithPrevBlock(block: Block, prevBlock: Block) = {
    block.version <= Block.CURRENT_BLOCK_VERSION && block.version > 0 && block.prevHash.toSeq == prevBlock.hash.toSeq &&
      block.timestamp.compareTo(prevBlock.timestamp) >= 0
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

  def deleteChainFrom(i: Int): Unit = synchronized {
    val chainDir = chainFileOps.getChainDir(nodeName)
    if (chainFileOps.isChainDirExists(nodeName)) {
      i until size map { n =>
        chainFileOps.deleteBlock(n, chainDir)
      }
    }
  }

  def addFactToNewBlock(signedStatement: SignedStatementMessage): Unit = {
    val fact = Fact(signedStatement.statement, signedStatement.providedSignaturesForKeys)
    val serializedFact = JsonSerializer.serialize(fact).getBytes
    val newBlock = genNextBlock(serializedFact)
    add(newBlock)
    writeChain()
  }

  def containsFactInside(newBlock: Block): Boolean = {
    extractFact(newBlock) match {
      case Right(newFact) =>
        containsFactInside(newFact.statement)
      case Left(_) => false
    }
  }

  def containsFactInside(statement: Statement): Boolean = {
    blocksFrom (0).find { block =>
      extractFact(block) match {
        case Right(fact) => statement == fact.statement
        case Left(_) => false
      }
    } nonEmpty
  }

  private def extractFact(block: Block) = FactJson.deserialize(new String(block.data))
}
