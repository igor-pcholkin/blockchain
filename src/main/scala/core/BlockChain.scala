package core

import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedDeque

class BlockChain {
  val origin = Block(0, Array[Byte](), LocalDateTime.of(2018, 12, 11, 17, 40, 0), "Future is here".getBytes)
  val chain = new ConcurrentLinkedDeque[Block]()
  chain.add(origin)

  def genNextBlock(data: Array[Byte]) = {
    val prevBlock = getLatestBlock
    val nextIndex = prevBlock.index + 1
    val prevHash = SHA256.hash(prevBlock)
    val nextTimestamp = LocalDateTime.now()
    Block(nextIndex, prevHash, nextTimestamp, data)
  }

  def add(block: Block) = {
    if (isValid(block))
      chain.add(block)
  }

  def isValid(block: Block) = {
    block.index == getLatestBlock.index + 1 && block.prevHash.toSeq == getLatestBlock.hash.toSeq &&
      block.timestamp.compareTo(getLatestBlock.timestamp) > 0
  }

  def getLatestBlock = chain.peekLast()

  def serialize = {
    val sb = new StringBuilder
    val it = chain.iterator
    while (it.hasNext) {
      sb.append(it.next)
      if (it.hasNext)
        sb.append(",")
    }
    sb.toString
  }
}
