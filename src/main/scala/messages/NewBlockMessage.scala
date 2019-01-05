package messages

import core.{Block, Message}

case class NewBlockMessage(block: Block, blockNo: Int) extends Message