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

  def getLatestBlock = chain.peekLast()
}
