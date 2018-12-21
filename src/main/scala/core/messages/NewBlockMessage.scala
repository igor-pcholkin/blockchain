package core.messages

import core.Block

case class NewBlockMessage(block: Block) extends Message {
  override def dataToSign: Array[Byte] = Array[Byte]()
}
