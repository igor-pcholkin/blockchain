package core

import java.time.LocalDateTime
import java.util.concurrent.ConcurrentLinkedDeque

class BlockChain {
  val chain = new ConcurrentLinkedDeque[Block]

  def genNextBlock(data: Array[Byte]) = {
    val prevBlock = getLatestBlock
    val nextIndex = prevBlock.index + 1
    val prevHash = SHA256.hash(prevBlock)
    val nextTimestamp = LocalDateTime.now()
    Block(nextIndex, prevHash, nextTimestamp, data)
  }

  def getLatestBlock = chain.peekLast()
}
